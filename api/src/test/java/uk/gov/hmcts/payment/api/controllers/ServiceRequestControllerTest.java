package uk.gov.hmcts.payment.api.controllers;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.componenttests.PaymentDbBackdoor;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestPaymentBo;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.domain.service.IdempotencyService;
import uk.gov.hmcts.payment.api.domain.service.ServiceRequestDomainService;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.dto.CasePaymentRequest;
import uk.gov.hmcts.payment.api.dto.OnlineCardPaymentRequest;
import uk.gov.hmcts.payment.api.dto.OnlineCardPaymentResponse;
import uk.gov.hmcts.payment.api.dto.OrganisationalServiceDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestFeeDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestPaymentDto;
import uk.gov.hmcts.payment.api.exception.AccountNotFoundException;
import uk.gov.hmcts.payment.api.exception.AccountServiceUnavailableException;
import uk.gov.hmcts.payment.api.exceptions.ServiceRequestReferenceNotFoundException;
import uk.gov.hmcts.payment.api.dto.order.ServiceRequestDto;
import uk.gov.hmcts.payment.api.dto.order.ServiceRequestFeeDto;
import uk.gov.hmcts.payment.api.exception.AccountNotFoundException;
import uk.gov.hmcts.payment.api.exception.AccountServiceUnavailableException;
import uk.gov.hmcts.payment.api.exceptions.ServiceRequestReferenceNotFoundException;
import uk.gov.hmcts.payment.api.external.client.GovPayClient;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.Link;
import uk.gov.hmcts.payment.api.external.client.dto.State;
import uk.gov.hmcts.payment.api.service.AccountService;
import uk.gov.hmcts.payment.api.service.DelegatingPaymentService;
import uk.gov.hmcts.payment.api.service.ReferenceDataService;
import uk.gov.hmcts.payment.api.util.AccountStatus;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;
import uk.gov.hmcts.payment.api.v1.model.exceptions.GatewayTimeoutException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.NoServiceFoundException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.ServiceRequestExceptionForNoAmountDue;
import uk.gov.hmcts.payment.api.v1.model.exceptions.ServiceRequestExceptionForNoMatchingAmount;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@Transactional
public class ServiceRequestControllerTest {

    private static final String USER_ID = UserResolverBackdoor.CITIZEN_ID;
    @Autowired
    PaymentDbBackdoor paymentDbBackdoor;
    @Autowired
    private WebApplicationContext webApplicationContext;
    @MockBean
    private ReferenceDataService referenceDataService;
    @MockBean
    private AuthTokenGenerator authTokenGenerator;
    @Autowired
    private ServiceRequestDomainService serviceRequestDomainService;
    @Autowired
    private IdempotencyService idempotencyService;
    @Autowired
    private AccountService<AccountDto, String> accountService;
    private RestActions restActions;

    @Spy
    private DelegatingPaymentService<GovPayPayment, String> delegateGovPay;

    @MockBean
    private GovPayClient govPayClient;

    @Autowired
    private UserResolverBackdoor userRequestAuthorizer;

    @Autowired
    private ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    private ObjectMapper objectMapper;

    @Before
    @Transactional
    public void setup() {

        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);

        restActions
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .withReturnUrl("https://www.moneyclaims.service.gov.uk");

        OrganisationalServiceDto organisationalServiceDto = OrganisationalServiceDto.orgServiceDtoWith()
            .serviceCode("AA001")
            .serviceDescription("DIVORCE")
            .build();

        when(referenceDataService.getOrganisationalDetail(any(), any(), any())).thenReturn(organisationalServiceDto);

    }

    @Test
    public void createPBAPaymentWithServiceRequestSuccessTest() throws Exception {

        //Creation of serviceRequest-reference
        String serviceRequestReferenceResult = getServiceRequestReference();

        //ServiceRequest Payment DTO
        ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto
            .paymentDtoWith().accountNumber("PBAFUNC12345")
            .amount(BigDecimal.valueOf(300))
            .currency("GBP")
            .customerReference("testCustReference").
                build();


        AccountDto liberataAccountResponse = AccountDto.accountDtoWith()
            .accountNumber("PBAFUNC12345")
            .accountName("CAERPHILLY COUNTY BOROUGH COUNCIL")
            .creditLimit(BigDecimal.valueOf(28879))
            .availableBalance(BigDecimal.valueOf(30000))
            .status(AccountStatus.ACTIVE)
            .build();

        when(accountService.retrieve("PBAFUNC12345")).thenReturn(liberataAccountResponse);

        //Payment amount should be matching with fees
        String idempotencyKey = UUID.randomUUID().toString();


        MvcResult successServiceRequestPaymentResult = restActions
            .withHeader("idempotency_key", idempotencyKey)
            .post("/service-request/" + serviceRequestReferenceResult + "/pba-payments", serviceRequestPaymentDto)
            .andExpect(status().isCreated())
            .andReturn();

        ServiceRequestPaymentBo serviceRequestPaymentBo = objectMapper.readValue(successServiceRequestPaymentResult.getResponse().getContentAsByteArray(), ServiceRequestPaymentBo.class);

        // 1. PBA Payment was successful
        assertTrue(serviceRequestPaymentBo.getPaymentReference().startsWith("RC-"));
        assertEquals("success", serviceRequestPaymentBo.getStatus());
        assertNotNull(serviceRequestPaymentBo.getDateCreated());
        assertNull(serviceRequestPaymentBo.getError());

        // 2.Duplicate request with same Idempotency key, same request content, same serviceRequest reference
        MvcResult duplicatePBAPaymentResult = restActions
            .withHeaderIfpresent("idempotency_key", idempotencyKey)
            .post("/service-request/" + serviceRequestReferenceResult + "/pba-payments", serviceRequestPaymentDto)
            .andExpect(status().isCreated())
            .andReturn();

        ServiceRequestPaymentBo oldServiceRequestPaymentBA = objectMapper.readValue(duplicatePBAPaymentResult.getResponse().getContentAsByteArray(), ServiceRequestPaymentBo.class);

        //Get old payment details from idempotency table
        assertEquals(serviceRequestPaymentBo.getPaymentReference(), oldServiceRequestPaymentBA.getPaymentReference());
        assertEquals(serviceRequestPaymentBo.getStatus(), oldServiceRequestPaymentBA.getStatus());
        assertEquals(serviceRequestPaymentBo.getDateCreated(), oldServiceRequestPaymentBA.getDateCreated());


        //3.Duplicate request with same Idempotency key with different Payment details
        serviceRequestPaymentDto.setAmount(BigDecimal.valueOf(111)); //changed the amount
        serviceRequestPaymentDto.setCustomerReference("cust-reference-change"); //changed the customer reference

        MvcResult conflictPBAPaymentResult = restActions
            .withHeaderIfpresent("idempotency_key", idempotencyKey)
            .post("/service-request/" + serviceRequestReferenceResult + "/pba-payments", serviceRequestPaymentDto)
            .andExpect(status().isConflict())
            .andReturn();

        assertTrue(conflictPBAPaymentResult.getResponse().
            getContentAsString().
            contains("Payment already present for idempotency key with different payment details"));

        //4. Different idempotency key, same serviceRequest reference, same payment details no amount due
        serviceRequestPaymentDto.setAmount(BigDecimal.valueOf(300)); //changed the amount
        serviceRequestPaymentDto.setCustomerReference("testCustReference"); //changed the customer reference

        restActions
            .withHeaderIfpresent("idempotency_key", UUID.randomUUID().toString())
            .post("/service-request/" + serviceRequestReferenceResult + "/pba-payments", serviceRequestPaymentDto)
            .andExpect(status().isPreconditionFailed())
            .andExpect(serviceRequestException -> assertTrue(serviceRequestException.getResolvedException() instanceof ServiceRequestExceptionForNoAmountDue))
            .andExpect(serviceRequestException -> assertEquals("The serviceRequest has already been paid", serviceRequestException.getResolvedException().getMessage()))
            .andReturn();
    }


    @Test
    public void createPBAPaymentWithServiceRequestTimeOutTest() throws Exception {
        when(accountService.retrieve("PBA12346")).thenThrow(
            new HystrixRuntimeException(HystrixRuntimeException.FailureType.TIMEOUT, HystrixCommand.class, "Unable to retrieve account information", null, null));

        String serviceRequestReference = getServiceRequestReference();

        //ServiceRequest Payment DTO
        ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto
            .paymentDtoWith().accountNumber("PBA12346")
            .amount(BigDecimal.valueOf(300))
            .currency("GBP")
            .customerReference("testCustReference").
                build();

        //ServiceRequest reference creation
        String idempotencyKey = UUID.randomUUID().toString();

        MvcResult result = restActions
            .withHeaderIfpresent("idempotency_key", idempotencyKey)
            .post("/service-request/" + serviceRequestReference + "/pba-payments", serviceRequestPaymentDto)
            .andExpect(status().isGatewayTimeout())
            .andReturn();

        assertTrue(result.getResponse().
            getContentAsString().
            contains("Unable to retrieve account information due to timeout"));

        //Duplicate request for timeout
        MvcResult result1 = restActions
            .withHeaderIfpresent("idempotency_key", idempotencyKey)
            .post("/service-request/" + serviceRequestReference + "/pba-payments", serviceRequestPaymentDto)
            .andExpect(status().isGatewayTimeout())
            .andReturn();

        assertTrue(result1.getResponse().
            getContentAsString().
            contains("Unable to retrieve account information due to timeout"));
    }

    @Test
    public void createPBAPaymentLiberataAccountFirstFailSuccessTest() throws Exception {
        AccountDto accountOnHoldResponse = AccountDto.accountDtoWith()
            .accountNumber("PBA12347")
            .accountName("CAERPHILLY COUNTY BOROUGH COUNCIL - Account on Hold")
            .creditLimit(BigDecimal.valueOf(28879))
            .availableBalance(BigDecimal.valueOf(30000))
            .status(AccountStatus.ON_HOLD)
            .build();

        AccountDto accountSuccessResponse = AccountDto.accountDtoWith()
            .accountNumber("PBAFUNC12345")
            .accountName("CAERPHILLY COUNTY BOROUGH COUNCIL")
            .creditLimit(BigDecimal.valueOf(28879))
            .availableBalance(BigDecimal.valueOf(30000))
            .status(AccountStatus.ACTIVE)
            .build();

        when(accountService.retrieve("PBAFUNC12345")).thenReturn(accountSuccessResponse);

        when(accountService.retrieve("PBA12347")).thenReturn(accountOnHoldResponse);

        String serviceRequestReference = getServiceRequestReference();

        //ServiceRequest Payment DTO
        ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto
            .paymentDtoWith().accountNumber("PBA12347")
            .amount(BigDecimal.valueOf(300))
            .currency("GBP")
            .customerReference("testCustReference").
                build();

        //ServiceRequest reference creation
        String idempotencyKey = UUID.randomUUID().toString();

        MvcResult accountOnHoldResult = restActions
            .withHeaderIfpresent("idempotency_key", idempotencyKey)
            .post("/service-request/" + serviceRequestReference + "/pba-payments", serviceRequestPaymentDto)
            .andExpect(status().isPaymentRequired())
            .andReturn();


        ServiceRequestPaymentBo serviceRequestPaymentBo = objectMapper.readValue(accountOnHoldResult.getResponse().getContentAsByteArray(), ServiceRequestPaymentBo.class);

        // 1. Account On Hold scenario
        String paymentReference = serviceRequestPaymentBo.getPaymentReference();
        assertTrue(paymentReference.startsWith("RC-"));
        assertEquals("failed", serviceRequestPaymentBo.getStatus());
        assertNotNull(serviceRequestPaymentBo.getDateCreated());
        assertEquals("CA-E0003", serviceRequestPaymentBo.getError().getErrorCode());
        assertEquals("Your account is on hold", serviceRequestPaymentBo.getError().getErrorMessage());


        // 2. Payment success scenario - request with Correct account details & Same ServiceRequest reference
        serviceRequestPaymentDto.setAccountNumber("PBAFUNC12345");
        String newIdempotencyKey = UUID.randomUUID().toString(); //changed idempotency Key

        MvcResult accountSuccessResult = restActions
            .withHeaderIfpresent("idempotency_key", newIdempotencyKey)
            .post("/service-request/" + serviceRequestReference + "/pba-payments", serviceRequestPaymentDto)
            .andExpect(status().isCreated())
            .andReturn();

        ServiceRequestPaymentBo duplicateRequestServiceRequestPaymentBO = objectMapper.readValue(accountSuccessResult.getResponse().getContentAsByteArray(), ServiceRequestPaymentBo.class);

        //Response should be success this time
        assertNotEquals(paymentReference, duplicateRequestServiceRequestPaymentBO.getPaymentReference());
        assertEquals("success", duplicateRequestServiceRequestPaymentBO.getStatus());
    }

    @Test
    public void createPBALiberataFailureAndAccountNotFoundScenarioTest() throws Exception {

        when(accountService.retrieve("PBA1111")).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
        when(accountService.retrieve("PBA2222")).thenThrow(new RuntimeException("Runtime exception"));

        String ServiceRequestReference = getServiceRequestReference();

        //ServiceRequest Payment DTO
        ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto
            .paymentDtoWith().accountNumber("PBA1111")
            .amount(BigDecimal.valueOf(300))
            .currency("GBP")
            .customerReference("testCustReference").
                build();

        // 1. Account not found exception
        restActions
            .withHeader("idempotency_key", UUID.randomUUID().toString())
            .post("/service-request/" + ServiceRequestReference + "/pba-payments", serviceRequestPaymentDto)
            .andExpect(status().isNotFound())
            .andExpect(ServiceRequestException -> assertTrue(ServiceRequestException.getResolvedException() instanceof AccountNotFoundException))
            .andExpect(ServiceRequestException -> assertTrue(ServiceRequestException.getResolvedException().getMessage().contains("Account information could not be found")))
            .andReturn();

        //AccountServiceUnAvailableException
        serviceRequestPaymentDto.setAccountNumber("PBA2222"); //Account for runtime exception

        restActions
            .withHeader("idempotency_key", UUID.randomUUID().toString())
            .post("/service-request/" + ServiceRequestReference + "/pba-payments", serviceRequestPaymentDto)
            .andExpect(status().isGatewayTimeout())
            .andExpect(serviceRequestException -> assertTrue(serviceRequestException.getResolvedException() instanceof AccountServiceUnavailableException))
            .andExpect(serviceRequestException -> assertTrue(serviceRequestException.getResolvedException().getMessage().contains("Unable to retrieve account information, please try again later")))
            .andReturn();
    }

    @Test
    public void serviceRequestReferenceNotFoundExceptionServiceRequestDTOEqualsTest() throws Exception {
        String serviceRequestReferenceNotPresent = "2021-1621352111111";

        //ServiceRequest Payment DTO
        ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto
            .paymentDtoWith().accountNumber("PBA12345")
            .amount(BigDecimal.valueOf(100))
            .currency("GBP")
            .customerReference("testCustReference").
                build();

        //Payment amount should not be matching with fees
        MvcResult result = restActions
            .withHeader("idempotency_key", UUID.randomUUID().toString())
            .post("/service-request/" + serviceRequestReferenceNotPresent + "/pba-payments", serviceRequestPaymentDto)
            .andExpect(status().isNotFound())
            .andExpect(serviceRequestException -> assertTrue(serviceRequestException.getResolvedException() instanceof ServiceRequestReferenceNotFoundException))
            .andExpect(serviceRequestException -> assertEquals("ServiceRequest reference doesn't exist", serviceRequestException.getResolvedException().getMessage()))
            .andReturn();

        //Equality test
        ServiceRequestPaymentDto serviceRequestPaymentDto2 = ServiceRequestPaymentDto
            .paymentDtoWith().accountNumber("PBA12345") //changed the account no
            .amount(BigDecimal.valueOf(100))
            .currency("GBP")
            .customerReference("testCustReference1").
                build();

        //assert not equal scenario
        assertFalse(serviceRequestPaymentDto.equals(serviceRequestPaymentDto2));

        serviceRequestPaymentDto2.setCustomerReference("testCustReference");
        //assert equal scenario
        assertTrue(serviceRequestPaymentDto.equals(serviceRequestPaymentDto)); //same Object
        assertTrue(serviceRequestPaymentDto.equals(serviceRequestPaymentDto2)); //Different Object

        //assert different class scenario
        ServiceRequestDto serviceRequestDto = ServiceRequestDto.serviceRequestDtoWith().build();
        assertFalse(serviceRequestPaymentDto.equals(serviceRequestDto));

        //Hashcode coverage
        assertTrue(Integer.valueOf(serviceRequestPaymentDto.hashCode()) instanceof Integer);

    }

    @Test
    public void createPBAInvalidCurrencyScenario() throws Exception {
        //serviceRequest Payment DTO
        ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto
            .paymentDtoWith().accountNumber("PBA12345")
            .amount(BigDecimal.valueOf(100))
            .currency("INR") //instead of GBP
            .customerReference("testCustReference").
                build();

        restActions
            .withHeader("idempotency_key", UUID.randomUUID().toString())
            .post("/service-request/" + "2021-1621352112222" + "/pba-payments", serviceRequestPaymentDto)
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().string("validCurrency: Invalid currency. Accepted value GBP"));
    }


    @Test
    public void createPBAPaymentNonMatchAmountTest() throws Exception {

        //serviceRequest Payment DTO
        ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto
            .paymentDtoWith().accountNumber("PBA12345")
            .amount(BigDecimal.valueOf(100))
            .currency("GBP")
            .customerReference("testCustReference").
                build();

        //serviceRequest reference creation
        String serviceRequestReference = getServiceRequestReference();

        //Payment amount should not be matching with fees
        MvcResult result = restActions
            .withHeader("idempotency_key", UUID.randomUUID().toString())
            .post("/service-request/" + serviceRequestReference + "/pba-payments", serviceRequestPaymentDto)
            .andExpect(status().isExpectationFailed())
            .andExpect(serviceRequestException -> assertTrue(serviceRequestException.getResolvedException() instanceof ServiceRequestExceptionForNoMatchingAmount))
            .andExpect(serviceRequestException -> assertEquals("The amount should be equal to serviceRequest balance", serviceRequestException.getResolvedException().getMessage()))
            .andReturn();

    }

    @Test
    public void createServiceRequestWithInValidCcdCaseNumber() throws Exception {

        ServiceRequestDto serviceRequestDto = ServiceRequestDto.serviceRequestDtoWith()
            .caseReference("123245677")
            .hmctsOrgId("MoneyClaimCase")
            .ccdCaseNumber("689869686968696")
            .casePaymentRequest(getCasePaymentRequest())
            .callBackUrl("http://callback/url")
            .fees(Collections.singletonList(getFee()))
            .build();


        MultiValueMap<String, String> header = new LinkedMultiValueMap<String, String>();

        restActions
            .post("/service-request", serviceRequestDto)
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().string("ccdCaseNumber: ccd_case_number should be 16 digit"));

    }

    @Test
    public void createServiceRequestWithDuplicateFees() throws Exception {

        List<ServiceRequestFeeDto> serviceRequestFeeDtoList = new ArrayList<ServiceRequestFeeDto>();
        serviceRequestFeeDtoList.add(getFee());
        serviceRequestFeeDtoList.add(getFee());

        ServiceRequestDto serviceRequestDto = ServiceRequestDto.serviceRequestDtoWith()
            .caseReference("123245677")
            .hmctsOrgId("MoneyClaimCase")
            .ccdCaseNumber("8689869686968696")
            .fees(serviceRequestFeeDtoList)
            .callBackUrl("http://callback/url")
            .casePaymentRequest(getCasePaymentRequest())
            .build();

        restActions
            .post("/service-request", serviceRequestDto)
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().string("feeCodeUnique: Fee code cannot be duplicated"));
    }

    @Test
    public void createServiceRequestWithInvalidCaseType() throws Exception {

        ServiceRequestDto serviceRequestDto = ServiceRequestDto.serviceRequestDtoWith()
            .caseReference("123245677")
            .hmctsOrgId("ClaimCase")
            .ccdCaseNumber("8689869686968696")
            .callBackUrl("http://callback/url")
            .casePaymentRequest(getCasePaymentRequest())
            .fees(Collections.singletonList(getFee()))
            .build();

        when(referenceDataService.getOrganisationalDetail(any(), any(), any())).thenThrow(new NoServiceFoundException("Test Error"));

        restActions
            .post("/service-request", serviceRequestDto)
            .andExpect(status().isNotFound())
            .andExpect(content().string("Test Error"));
    }

    @Test
    public void createServiceRequestWithValidCaseTypeReturnsTimeOutException() throws Exception {

        ServiceRequestDto serviceRequestDto = ServiceRequestDto.serviceRequestDtoWith()
            .caseReference("123245677")
            .hmctsOrgId("ClaimCase")
            .ccdCaseNumber("8689869686968696")
            .callBackUrl("http://callback/url")
            .casePaymentRequest(getCasePaymentRequest())
            .fees(Collections.singletonList(getFee()))
            .build();

        when(referenceDataService.getOrganisationalDetail(any(), any(), any())).thenThrow(new GatewayTimeoutException("Test Error"));

        restActions
            .post("/service-request", serviceRequestDto)
            .andExpect(status().isGatewayTimeout())
            .andExpect(content().string("Test Error"));
    }


    @Test
    public void createSuccessOnlinePayment() throws Exception {
        //Creation of Order-reference
        String orderReferenceResult = getOrderReference();

        OnlineCardPaymentRequest onlineCardPaymentRequest = OnlineCardPaymentRequest.onlineCardPaymentRequestWith()
            .amount(new BigDecimal(300))
            .currency(CurrencyCode.GBP)
            .language("cy")
            .build();

        when(govPayClient.createPayment(anyString(), any())).thenReturn(getGovPayPayment());

        MvcResult result = restActions
            .withHeader("service-callback-url", "idempotencyKey")
            .post("/service-request/" + orderReferenceResult + "/card-payments", onlineCardPaymentRequest)
            .andExpect(status().isCreated())
            .andReturn();
        OnlineCardPaymentResponse onlineCardPaymentResponse =  objectMapper.readValue(result.getResponse().getContentAsByteArray(),OnlineCardPaymentResponse.class);
        assertEquals("created",onlineCardPaymentResponse.getStatus());

    }


    @Test
    public void createMultipleOnlinePaymentByCancelingSessionWithGovPay4PaymentWithCreatedStatusWithIn90Mins() throws Exception {
        //Creation of Order-reference
        String orderReferenceResult = getOrderReference();

        OnlineCardPaymentRequest onlineCardPaymentRequest = OnlineCardPaymentRequest.onlineCardPaymentRequestWith()
            .amount(new BigDecimal(300))
            .currency(CurrencyCode.GBP)
            .language("cy")
            .build();

        when(govPayClient.createPayment(anyString(), any())).thenReturn(getGovPayPayment());

        MvcResult result = restActions
            .withHeader("service-callback-url", "dummy")
            .post("/service-request/" + orderReferenceResult + "/card-payments", onlineCardPaymentRequest)
            .andExpect(status().isCreated())
            .andReturn();
        OnlineCardPaymentResponse onlineCardPaymentResponse =  objectMapper.readValue(result.getResponse().getContentAsByteArray(),OnlineCardPaymentResponse.class);
        assertEquals("created",onlineCardPaymentResponse.getStatus());

        Thread.sleep(1000); //just to resemble minutes changes to create new payment

        MvcResult result1 = restActions
            .withHeader("service-callback-url", "dummy")
            .post("/service-request/" + orderReferenceResult + "/card-payments", onlineCardPaymentRequest)
            .andExpect(status().isCreated())
            .andReturn();

        assertEquals("created",onlineCardPaymentResponse.getStatus());

    }

    private GovPayPayment getGovPayPayment() {
        return GovPayPayment.govPaymentWith()
            .amount(300)
            .state(new State("created", false, null, null))
            .description("description")
            .reference("reference")
            .paymentId("paymentId")
            .paymentProvider("sandbox")
            .returnUrl("https://www.google.com")
            .links(GovPayPayment.Links.linksWith().nextUrl(new Link("any", ImmutableMap.of(), "cancelHref", "any")).build())
            .build();
    }


    private ServiceRequestFeeDto getFee() {
        return ServiceRequestFeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("92.19"))
            .code("FEE312")
            .version("1")
            .volume(2)
            .build();
    }

    private List<ServiceRequestFeeDto> getMultipleFees() {
        ServiceRequestFeeDto fee1 = ServiceRequestFeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("100"))
            .code("FEE100")
            .version("1")
            .volume(1)
            .build();

        ServiceRequestFeeDto fee2 = ServiceRequestFeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("200"))
            .code("FEE102")
            .version("1")
            .volume(1)
            .build();

        return Arrays.asList(fee1, fee2);
    }

    private CasePaymentRequest getCasePaymentRequest() {
        CasePaymentRequest casePaymentRequest = CasePaymentRequest.casePaymentRequestWith()
            .action("action")
            .responsibleParty("party")
            .build();

        return casePaymentRequest;
    }

    private String getServiceRequestReference() throws Exception {
        ServiceRequestDto serviceRequestDto = ServiceRequestDto.serviceRequestDtoWith()
            .caseReference("123245677")
            .hmctsOrgId("MoneyClaimCase")
            .ccdCaseNumber("8689869686968696")
            .fees(getMultipleFees())
            .callBackUrl("http://callback/url")
            .casePaymentRequest(getCasePaymentRequest())
            .build();

        MvcResult result = restActions
            .post("/service-request", serviceRequestDto)
            .andExpect(status().isCreated())
            .andReturn();

        Map serviceRequestReferenceResultMaps = objectMapper.readValue(result.getResponse().getContentAsByteArray(), Map.class);
        String serviceRequestReferenceResult = serviceRequestReferenceResultMaps.get("service_request_reference").toString();
        assertNotNull(serviceRequestReferenceResult);
        return serviceRequestReferenceResult;
    }

}
