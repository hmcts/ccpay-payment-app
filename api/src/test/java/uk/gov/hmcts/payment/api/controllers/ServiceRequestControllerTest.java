package uk.gov.hmcts.payment.api.controllers;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
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
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.domain.model.Error;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestPaymentBo;
import uk.gov.hmcts.payment.api.domain.service.IdempotencyService;
import uk.gov.hmcts.payment.api.domain.service.ServiceRequestDomainService;
import uk.gov.hmcts.payment.api.dto.*;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestFeeDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestPaymentDto;
import uk.gov.hmcts.payment.api.exception.AccountNotFoundException;
import uk.gov.hmcts.payment.api.exception.AccountServiceUnavailableException;
import uk.gov.hmcts.payment.api.exceptions.ServiceRequestReferenceNotFoundException;
import uk.gov.hmcts.payment.api.external.client.GovPayClient;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.Link;
import uk.gov.hmcts.payment.api.external.client.dto.State;
import uk.gov.hmcts.payment.api.model.FeePayApportion;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.service.AccountService;
import uk.gov.hmcts.payment.api.service.DelegatingPaymentService;
import uk.gov.hmcts.payment.api.service.FeesService;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.service.ReferenceDataService;
import uk.gov.hmcts.payment.api.util.AccountStatus;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;
import uk.gov.hmcts.payment.api.v1.model.exceptions.GatewayTimeoutException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
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
    private IdempotencyService idempotencyService;
    @Autowired
    private AccountService<AccountDto, String> accountService;
    private RestActions restActions;

    @Autowired
    private UserResolverBackdoor userRequestAuthorizer;

    @Autowired
    private ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    ServiceRequestDomainService serviceRequestDomainService;

    @MockBean
    private PaymentService<PaymentFeeLink, String> paymentService;

    @MockBean
    private DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService;

    @MockBean
    private GovPayClient govPayClient;

    @MockBean
    private FeesService feesService;

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

        when(referenceDataService.getOrganisationalDetail(any(),any(), any())).thenReturn(organisationalServiceDto);

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


        ServiceRequestPaymentBo serviceRequestPaymentBo = ServiceRequestPaymentBo.serviceRequestPaymentBoWith().
            paymentReference("RC-reference").
            dateCreated("20-09-2021").
            status("success").
            build();

        ResponseEntity<ServiceRequestPaymentBo> responseEntity =
            new ResponseEntity<>(objectMapper.readValue("{\"response_body\":\"response_body\"}", ServiceRequestPaymentBo.class), HttpStatus.CREATED);

        ResponseEntity<ServiceRequestPaymentBo> responseEntity2 =
                    new ResponseEntity<>(objectMapper.readValue("{\"response_body\":\"response_body\"}", ServiceRequestPaymentBo.class), HttpStatus.CONFLICT);

        ResponseEntity<ServiceRequestPaymentBo> responseEntity3 =
                            new ResponseEntity<>(objectMapper.readValue("{\"response_body\":\"response_body\"}", ServiceRequestPaymentBo.class), HttpStatus.PRECONDITION_FAILED);

        when(serviceRequestDomainService.addPayments(any(),any())).thenReturn(serviceRequestPaymentBo);

        when(serviceRequestDomainService.createIdempotencyRecord(any(),any(),any(),any(),any(),any())).
            thenReturn(responseEntity, responseEntity, responseEntity2,responseEntity3);

        ServiceRequestResponseDto serviceRequestResponseDtoSample = ServiceRequestResponseDto.serviceRequestResponseDtoWith()
            .serviceRequestReference("2021-1632746723494").build();

        when(serviceRequestDomainService.create(any(),any())).thenReturn(serviceRequestResponseDtoSample);

        MvcResult successServiceRequestPaymentResult = restActions
            .withHeader("idempotency_key", idempotencyKey)
            .post("/service-request/" + serviceRequestReferenceResult + "/pba-payments", serviceRequestPaymentDto)
            .andExpect(status().isCreated())
            .andReturn();

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

        ServiceRequestPaymentBo oldServiceRequestPaymentBA = ServiceRequestPaymentBo.serviceRequestPaymentBoWith().
            paymentReference("RC-reference").
            dateCreated("20-09-2021").
            status("success").
            build();

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

        //4. Different idempotency key, same serviceRequest reference, same payment details no amount due
        serviceRequestPaymentDto.setAmount(BigDecimal.valueOf(300)); //changed the amount
        serviceRequestPaymentDto.setCustomerReference("testCustReference"); //changed the customer reference

        restActions
            .withHeaderIfpresent("idempotency_key", UUID.randomUUID().toString())
            .post("/service-request/" + serviceRequestReferenceResult + "/pba-payments", serviceRequestPaymentDto)
            .andExpect(status().isPreconditionFailed())
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

        ServiceRequestPaymentBo serviceRequestPaymentBoSample = ServiceRequestPaymentBo.serviceRequestPaymentBoWith().
            paymentReference("reference").
            status("failed").
            build();

        ResponseEntity<ServiceRequestPaymentBo> responseEntity =
            new ResponseEntity<>(objectMapper.readValue("{\"response_body\":\"response_body\"}", ServiceRequestPaymentBo.class), HttpStatus.GATEWAY_TIMEOUT);

        when(serviceRequestDomainService.addPayments(any(),any())).thenReturn(serviceRequestPaymentBoSample);

        when(serviceRequestDomainService.createIdempotencyRecord(any(),any(),any(),any(),any(),any())).thenReturn(responseEntity);

        ServiceRequestResponseDto serviceRequestResponseDtoSample = ServiceRequestResponseDto.serviceRequestResponseDtoWith()
            .serviceRequestReference("2021-1632746723494").build();

        when(serviceRequestDomainService.create(any(),any())).thenReturn(serviceRequestResponseDtoSample);

        when(serviceRequestDomainService.businessValidationForServiceRequests(any(),any())).
            thenThrow(new AccountServiceUnavailableException("Unable to retrieve account information due to timeout"));

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

        Error error = new Error();
        error.setErrorCode("CA-E0003");
        error.setErrorMessage("Your account is on hold");

        ServiceRequestPaymentBo serviceRequestPaymentBo = ServiceRequestPaymentBo.serviceRequestPaymentBoWith().
            paymentReference("RC-reference").
            dateCreated("20-09-2021").
            error(error).
            status("failed").
            build();

        ServiceRequestPaymentBo serviceRequestPaymentBo2 = ServiceRequestPaymentBo.serviceRequestPaymentBoWith().
            paymentReference("RC-reference2").
            status("success").
            build();

        ResponseEntity<ServiceRequestPaymentBo> responseEntity =
            new ResponseEntity<>(objectMapper.readValue("{\"response_body\":\"response_body\"}", ServiceRequestPaymentBo.class), HttpStatus.PAYMENT_REQUIRED);

        ResponseEntity<ServiceRequestPaymentBo> responseEntity2 =
            new ResponseEntity<>(objectMapper.readValue("{\"response_body\":\"response_body\"}", ServiceRequestPaymentBo.class), HttpStatus.CREATED);

        when(serviceRequestDomainService.addPayments(any(),any())).thenReturn(serviceRequestPaymentBo,serviceRequestPaymentBo2);

        when(serviceRequestDomainService.createIdempotencyRecord(any(),any(),any(),any(),any(),any())).thenReturn(responseEntity,responseEntity2);

        //ServiceRequest reference creation
        String idempotencyKey = UUID.randomUUID().toString();

        MvcResult accountOnHoldResult = restActions
            .withHeaderIfpresent("idempotency_key", idempotencyKey)
            .post("/service-request/" + serviceRequestReference + "/pba-payments", serviceRequestPaymentDto)
            .andExpect(status().isPaymentRequired())
            .andReturn();


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

        //Response should be success this time
        assertNotEquals(paymentReference, serviceRequestPaymentBo2.getPaymentReference());
        assertEquals("success", serviceRequestPaymentBo2.getStatus());
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

        ServiceRequestPaymentBo serviceRequestPaymentBoSample = ServiceRequestPaymentBo.serviceRequestPaymentBoWith().
            paymentReference("reference").
            status("failed").
            build();

        when(serviceRequestDomainService.addPayments(any(),any())).thenReturn(serviceRequestPaymentBoSample);

        ResponseEntity<ServiceRequestPaymentBo> responseEntity =
            new ResponseEntity<>(objectMapper.readValue("{\"response_body\":\"response_body\"}", ServiceRequestPaymentBo.class), HttpStatus.NOT_FOUND);

        ResponseEntity<ServiceRequestPaymentBo> responseEntity2 =
                    new ResponseEntity<>(objectMapper.readValue("{\"response_body\":\"response_body\"}", ServiceRequestPaymentBo.class), HttpStatus.GATEWAY_TIMEOUT);

        when(serviceRequestDomainService.createIdempotencyRecord(any(),any(),any(),any(),any(),any())).thenReturn(responseEntity,responseEntity2);

        when(serviceRequestDomainService.businessValidationForServiceRequests(any(),any())).
            thenThrow(new AccountNotFoundException("Account information could not be found"),
                new AccountServiceUnavailableException("Unable to retrieve account information, please try again later"));

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

        ServiceRequestPaymentBo serviceRequestPaymentBoSample = ServiceRequestPaymentBo.serviceRequestPaymentBoWith().
            paymentReference("reference").
            status("failed").
            build();

        when(serviceRequestDomainService.addPayments(any(),any())).thenReturn(serviceRequestPaymentBoSample);

        ResponseEntity<ServiceRequestPaymentBo> responseEntity =
            new ResponseEntity<>(objectMapper.readValue("{\"response_body\":\"response_body\"}", ServiceRequestPaymentBo.class), HttpStatus.NOT_FOUND);

        when(serviceRequestDomainService.createIdempotencyRecord(any(),any(),any(),any(),any(),any())).thenReturn(responseEntity);

        when(serviceRequestDomainService.businessValidationForServiceRequests(any(),any())).
            thenThrow(new ServiceRequestReferenceNotFoundException("ServiceRequest reference doesn't exist"));

        //Payment amount should not be matching with fees
        MvcResult result = restActions
            .withHeader("idempotency_key", UUID.randomUUID().toString())
            .post("/service-request/" + serviceRequestReferenceNotPresent + "/pba-payments", serviceRequestPaymentDto)
            .andExpect(status().isNotFound())
            .andExpect(serviceRequestException -> assertTrue(serviceRequestException.getResolvedException() instanceof ServiceRequestReferenceNotFoundException))
            .andExpect(serviceRequestException -> assertEquals("ServiceRequest reference doesn't exist", serviceRequestException.getResolvedException().getMessage()))
            .andExpect(orderException -> assertTrue(orderException.getResolvedException() instanceof ServiceRequestReferenceNotFoundException))
            .andExpect(orderException -> assertEquals("ServiceRequest reference doesn't exist", orderException.getResolvedException().getMessage()))
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

        ServiceRequestPaymentBo serviceRequestPaymentBoSample = ServiceRequestPaymentBo.serviceRequestPaymentBoWith().
            paymentReference("reference").
            status("failed").
            build();

        ResponseEntity<ServiceRequestPaymentBo> responseEntity =
            new ResponseEntity<>(objectMapper.readValue("{\"response_body\":\"response_body\"}", ServiceRequestPaymentBo.class), HttpStatus.EXPECTATION_FAILED);

        when(serviceRequestDomainService.addPayments(any(),any())).thenReturn(serviceRequestPaymentBoSample);

        when(serviceRequestDomainService.createIdempotencyRecord(any(),any(),any(),any(),any(),any())).thenReturn(responseEntity);

        //serviceRequest reference creation
        String serviceRequestReference = getServiceRequestReference();

        //Payment amount should not be matching with fees
        MvcResult result = restActions
            .withHeader("idempotency_key", UUID.randomUUID().toString())
            .post("/service-request/" + serviceRequestReference + "/pba-payments", serviceRequestPaymentDto)
            .andExpect(status().isExpectationFailed())
            .andReturn();

    }

    @Test
    public void createCardPaymentTest() throws Exception {

        String serviceRequestReference = getServiceRequestReference();

        OnlineCardPaymentRequest onlineCardPaymentRequest = OnlineCardPaymentRequest.onlineCardPaymentRequestWith()
            .amount(new BigDecimal(300))
            .currency(CurrencyCode.GBP)
            .language("cy")
            .build();

        OnlineCardPaymentResponse onlineCardPaymentResponse = OnlineCardPaymentResponse.onlineCardPaymentResponseWith()
                .paymentReference("RC-ref")
                    .build();

        when(serviceRequestDomainService.create(any(),any(),any(),any())).thenReturn(onlineCardPaymentResponse);

        MvcResult result = restActions
            .post("/service-request/" + serviceRequestReference + "/card-payments", onlineCardPaymentRequest)
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
    public void createServiceRequestWithValidCaseTypeReturnsTimeOutException() throws Exception {

        ServiceRequestDto serviceRequestDto = ServiceRequestDto.serviceRequestDtoWith()
            .caseReference("123245677")
            .hmctsOrgId("ClaimCase")
            .ccdCaseNumber("8689869686968696")
            .callBackUrl("http://callback/url")
            .casePaymentRequest(getCasePaymentRequest())
            .fees(Collections.singletonList(getFee()))
            .build();

        when(serviceRequestDomainService.create(any(),any())).thenThrow(new GatewayTimeoutException("Test Error"));

        doNothing().when(serviceRequestDomainService).sendMessageTopicCPO(any(ServiceRequestDto.class),any(Payment.class));

        restActions
            .post("/service-request", serviceRequestDto)
            .andExpect(status().isGatewayTimeout())
            .andExpect(content().string("Test Error"));
    }


    @Test
    public void createSuccessOnlinePayment() throws Exception {
        //Creation of Order-reference
        String orderReferenceResult = getServiceRequestReference();

        OnlineCardPaymentRequest onlineCardPaymentRequest = OnlineCardPaymentRequest.onlineCardPaymentRequestWith()
            .amount(new BigDecimal(300))
            .currency(CurrencyCode.GBP)
            .language("cy")
            .build();

        when(govPayClient.createPayment(anyString(), any())).thenReturn(getGovPayPayment());

        OnlineCardPaymentResponse onlineCardPaymentResponseSample = OnlineCardPaymentResponse.onlineCardPaymentResponseWith().status("created").build();

        when(serviceRequestDomainService.create(any(),any(),any(),any())).thenReturn(onlineCardPaymentResponseSample);


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
        String orderReferenceResult = getServiceRequestReference();

        OnlineCardPaymentRequest onlineCardPaymentRequest = OnlineCardPaymentRequest.onlineCardPaymentRequestWith()
            .amount(new BigDecimal(300))
            .currency(CurrencyCode.GBP)
            .language("cy")
            .build();

        when(govPayClient.createPayment(anyString(), any())).thenReturn(getGovPayPayment());

        OnlineCardPaymentResponse onlineCardPaymentResponseSample = OnlineCardPaymentResponse.onlineCardPaymentResponseWith().status("created").build();

        when(serviceRequestDomainService.create(any(),any(),any(),any())).thenReturn(onlineCardPaymentResponseSample);

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


    @Test
    public void createSuccessOnlinePaymentAndValidateSuccessStatus() throws Exception {

        Payment payment = Payment.paymentWith().internalReference("abc")
            .id(1)
            .reference("RC-1632-3254-9172-5888").paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
            .build();

        List<Payment> paymentList = new ArrayList<>();
        paymentList.add(payment);

        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith().ccdCaseNumber("1234")
            .enterpriseServiceName("divorce")
            .payments(paymentList)
            .build();

        when(paymentService.findPayment(anyString())).thenReturn(payment);
        when(paymentService.findByPaymentId(anyInt())).thenReturn(Arrays.asList(FeePayApportion.feePayApportionWith()
            .feeId(1)
            .build()));
        when(feesService.getPaymentFee(anyInt())).thenReturn(Optional.of(PaymentFee.feeWith().paymentLink(paymentFeeLink).build()));
        when(delegatingPaymentService.retrieve(any(PaymentFeeLink.class) ,anyString())).thenReturn(paymentFeeLink);
        MvcResult result1 = restActions
            .get("/card-payments/" + payment.getInternalReference() + "/status")
            .andExpect(status().isOk())
            .andReturn();
        PaymentDto paymentDto =  objectMapper.readValue(result1.getResponse().getContentAsByteArray(),PaymentDto.class);
        assertEquals("Success",paymentDto.getStatus());

    }

    @Test
    public void createSuccessOnlinePaymentAndValidateFailureStatus() throws Exception {

        Payment payment = Payment.paymentWith().internalReference("abc")
            .reference("RC-1632-3254-9172-5888").paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
            .build();

        List<Payment> paymentList = new ArrayList<>();
        paymentList.add(payment);

        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith().ccdCaseNumber("1234")
            .payments(paymentList)
            .build();

        when(paymentService.findPayment(anyString())).thenThrow(new PaymentNotFoundException());
        when(delegatingPaymentService.retrieve(anyString())).thenReturn(paymentFeeLink);
        MvcResult result1 = restActions
            .get("/card-payments/" + payment.getInternalReference() + "/status")
            .andExpect(status().isBadRequest())
            .andReturn();

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

    private CasePaymentRequest getCasePaymentRequest(){
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

        ServiceRequestResponseDto serviceRequestResponseDtoSample = ServiceRequestResponseDto.serviceRequestResponseDtoWith()
            .serviceRequestReference("2021-1632746723494").build();

        when(serviceRequestDomainService.create(any(),any())).thenReturn(serviceRequestResponseDtoSample);

        doNothing().when(serviceRequestDomainService).sendMessageTopicCPO(any(ServiceRequestDto.class), any(Payment.class));

        MvcResult result = restActions
            .post("/service-request", serviceRequestDto)
            .andExpect(status().isCreated())
            .andReturn();

        ServiceRequestResponseDto serviceRequestResponseDto = objectMapper.readValue(result.getResponse().getContentAsString(),
            ServiceRequestResponseDto.class);
        String serviceRequestReferenceResult = serviceRequestResponseDto.getServiceRequestReference();
        assertNotNull(serviceRequestReferenceResult);
        return serviceRequestReferenceResult;
    }

}


