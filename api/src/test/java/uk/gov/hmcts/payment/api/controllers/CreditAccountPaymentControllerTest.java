package uk.gov.hmcts.payment.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.val;
import org.apache.commons.lang3.RandomUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.fees2.register.api.contract.Fee2Dto;
import uk.gov.hmcts.fees2.register.api.contract.FeeVersionDto;
import uk.gov.hmcts.fees2.register.api.contract.Jurisdiction1Dto;
import uk.gov.hmcts.fees2.register.api.contract.Jurisdiction2Dto;
import uk.gov.hmcts.payment.api.componenttests.PaymentDbBackdoor;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.dto.OrganisationalServiceDto;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.liberata.PaymentAccountResponse;
import uk.gov.hmcts.payment.api.dto.liberata.PaymentAccountResponseStatus;
import uk.gov.hmcts.payment.api.dto.liberata.identity.TokenResponse;
import uk.gov.hmcts.payment.api.exception.AccountServiceUnavailableException;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentChannel;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentMethod;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.model.StatusHistory;
import uk.gov.hmcts.payment.api.reports.FeesService;
import uk.gov.hmcts.payment.api.service.AccountService;
import uk.gov.hmcts.payment.api.service.LiberataRealTimeAPI;
import uk.gov.hmcts.payment.api.service.ReferenceDataService;
import uk.gov.hmcts.payment.api.service.RefundRemissionEnableService;
import uk.gov.hmcts.payment.api.util.AccountStatus;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.CustomResultMatcher;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;
import uk.gov.hmcts.payment.api.v1.model.exceptions.GatewayTimeoutException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.NoServiceFoundException;
import uk.gov.hmcts.payment.referencedata.model.Site;
import uk.gov.hmcts.payment.referencedata.service.SiteService;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static uk.gov.hmcts.payment.api.dto.liberata.PaymentAccountResponseStatus.ACCOUNT_NOT_FOUND;
import static uk.gov.hmcts.payment.api.dto.liberata.PaymentAccountResponseStatus.EXCEEDED_CREDIT_LIMIT;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
public class CreditAccountPaymentControllerTest extends PaymentsDataUtil {

    private static final String USER_ID_PAYMENT_ROLE = UserResolverBackdoor.CASEWORKER_ID;
    private final static String PAYMENT_REFERENCE_REFEX = "^[RC-]{3}(\\w{4}-){3}(\\w{4})";
    private static final String USER_ID = UserResolverBackdoor.AUTHENTICATED_USER_ID;
    @Autowired
    protected ServiceResolverBackdoor serviceRequestAuthorizer;
    @Autowired
    protected UserResolverBackdoor userRequestAuthorizer;
    @Autowired
    protected PaymentDbBackdoor db;
    @Autowired
    protected Payment2Repository paymentRepository;
    @Autowired
    protected AccountService<AccountDto, String> accountService;

    @MockitoBean
    protected  LiberataRealTimeAPI liberataRealTimeAPI;

    @MockitoBean
    private SiteService<Site, String> siteServiceMock;
    @MockitoBean
    ReferenceDataService referenceDataService;
    RestActions restActions;
    MockMvc mvc;
    CreditAccountPaymentRequest request;
    @Autowired
    private ConfigurableListableBeanFactory configurableListableBeanFactory;
    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private LaunchDarklyFeatureToggler featureToggler;
    @MockitoBean
    private RefundRemissionEnableService refundRemissionEnableService;

    @MockitoBean
    private FeesService feesService;

    protected CustomResultMatcher body() {
        return new CustomResultMatcher(objectMapper);
    }

    @Before
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);
        mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);
        Mockito.when(liberataRealTimeAPI.getValidToken()).thenReturn(createTestTokenResponse());


        restActions
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .withReturnUrl("https://www.moneyclaims.service.gov.uk");

        Mockito.reset(accountService);
        request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFinRemJson().getBytes(), CreditAccountPaymentRequest.class);
    }

    public void setupForPaymentRoleUser() {
        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);

        restActions
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID_PAYMENT_ROLE)
            .withUserId(USER_ID_PAYMENT_ROLE)
            .withReturnUrl("https://www.moneyclaims.service.gov.uk");

        List<Site> serviceReturn = Arrays.asList(Site.siteWith()
                .sopReference("sop")
                .siteId("AA99")
                .name("name")
                .service("service")
                .id(1)
                .build(),
            Site.siteWith()
                .sopReference("sop")
                .siteId("AA001")
                .name("name")
                .service("service")
                .id(1)
                .build()
        );

        when(siteServiceMock.getAllSites()).thenReturn(serviceReturn);
    }

    @After
    public void tearDown() {
        request=null;
        this.restActions=null;
        mvc=null;
    }

    @Test
    public void createCreditAccountPaymentTest() throws Exception {

        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getSuccessPaymentAccountResponse());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());

        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isCreated());
    }

    @Test
    public void rejectDuplicatePayment_ccdCaseNumber() throws Exception {
        request.setCcdCaseNumber("CCD105");
        request.setCaseReference(null);

        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getSuccessPaymentAccountResponse());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());

        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isCreated());

        // 2nd request
        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isBadRequest());
    }

    @Test
    public void rejectDuplicatePayment_caseReference() throws Exception {

        request.setCcdCaseNumber(null);
        request.setCaseReference("33333");
        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getSuccessPaymentAccountResponse());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());

        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isCreated());

        // 2nd request
        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldNotRejectDuplicatePaymentIfAmountIsDifferent() throws Exception {

        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getSuccessPaymentAccountResponse());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());

        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isCreated());

        // different amount for the 2nd request
        request.setAmount(BigDecimal.valueOf(500.50));
        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isCreated());
    }

    @Test
    public void shouldNotRejectDuplicatePaymentIfFeeCodeIsDifferent() throws Exception {

        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);
        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getSuccessPaymentAccountResponse());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());

        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isCreated());

        // different fee code for the 2nd request
        val feeDto = request.getFees().getFirst();
        feeDto.setCode("X0102");
        feeDto.setVersion("1");
        feeDto.setCalculatedAmount(BigDecimal.valueOf(101.89));


        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isCreated());
    }

    @Test
    public void shouldNotRejectDuplicatePaymentIfFeeVersionIsDifferent() throws Exception {

        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getSuccessPaymentAccountResponse());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());

        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isCreated());

        // different fee version for the 2nd request
       val feeDto =  request.getFees().getFirst();
        feeDto.setCode("X0101");
        feeDto.setVersion("4");
        feeDto.setCalculatedAmount(BigDecimal.valueOf(101.89));
        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isCreated());
    }

    @Test
    public void retrieveCreditAccountPaymentByPaymentReference() throws Exception {
        //Create a payment in remissionDbBackdoor
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("11.99"))
            .caseReference("Reference1")
            .ccdCaseNumber("ccdCaseNumber1")
            .description("Description1")
            .serviceType("Probate")
            .currency("GBP")
            .siteId("AA01")
            .userId(USER_ID)
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .reference("RC-1519-9028-1909-3475")
            .build();
        PaymentFee fee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("11.99")).version("1").code("X0001").build();

        PaymentFeeLink paymentFeeLink = db.create(PaymentFeeLink.paymentFeeLinkWith().paymentReference("2018-15186162020").payments(Arrays.asList(payment)).fees(Arrays.asList(fee)));
        payment.setPaymentLink(paymentFeeLink);

        Payment savedPayment = paymentFeeLink.getPayments().get(0);

        MvcResult result = restActions
            .get("/credit-account-payments/RC-1519-9028-1909-3475")
            .andExpect(status().isOk())
            .andReturn();

        PaymentDto response = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(response);
        assertTrue(response.getReference().matches(PAYMENT_REFERENCE_REFEX));
        assertEquals(response.getAmount(), new BigDecimal("11.99"));
    }

    @Test
    public void validCreditAccountPaymentRequestJsonTest() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJson().getBytes(), CreditAccountPaymentRequest.class);
        assertNotNull(request);
    }

    @Test
    public void createCreditAccountPayment_withInvalidRequestJsonTest() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentInvalidRequestJson().getBytes(), CreditAccountPaymentRequest.class);

        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void createCreditAccountPayment_WithInvalidServiceNameTest() throws Exception {

        restActions
            .post(format("/credit-account-payments"), creditAccountPaymentRequestJsonWithInvalidServiceName())
            .andExpect(status().isBadRequest())
            .andReturn();

    }

    @Test
    public void retrievePaymentStatusesTest() throws Exception {
        //Create a payment in remissionDbBackdoor
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("11.99"))
            .caseReference("Reference11")
            .ccdCaseNumber("ccdCaseNumber11")
            .description("Description11")
            .serviceType("Probate")
            .currency("GBP")
            .siteId("AA011")
            .userId(USER_ID)
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .reference("RC-1519-9239-1920-0375")
            .statusHistories(Arrays.asList(StatusHistory.statusHistoryWith()
                .status("pending")
                .build()))
            .build();
        PaymentFee fee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("11.99")).version("1").code("X0001").build();
        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getSuccessPaymentAccountResponse());
        PaymentFeeLink paymentFeeLink = db.create(PaymentFeeLink.paymentFeeLinkWith().paymentReference("2018-15199028243").payments(Arrays.asList(payment)).fees(Arrays.asList(fee)));
        payment.setPaymentLink(paymentFeeLink);

        Payment savedPayment = paymentFeeLink.getPayments().get(0);

        MvcResult result = restActions
            .get("/credit-account-payments/RC-1519-9239-1920-0375/statuses")
            .andExpect(status().isOk())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(payment);
        assertEquals(paymentDto.getReference(), payment.getReference());
        paymentDto.getStatusHistories().stream().forEach(h -> {
            assertEquals(h.getStatus(), "Pending");
        });
    }

    @Test
    public void validateCreateCreditAccountPayment_withoutCcdCaseNumberAndCaseReferenceTest() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(jsonRequestWithoutCcdCaseRefAndCaseRef().getBytes(), CreditAccountPaymentRequest.class);

        MvcResult res = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        assertEquals("eitherOneRequired: Either ccdCaseNumber or caseReference is required.", res.getResponse().getContentAsString());
    }

    @Test
    public void validateCreateCreditAccountPayment_withoutSiteIdAndCaseType() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(jsonRequestWithoutSiteIdAndCaseType().getBytes(), CreditAccountPaymentRequest.class);

        MvcResult res = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        assertEquals("eitherIdOrTypeRequired: Either of Site ID or Case Type is mandatory as part of the request.", res.getResponse().getContentAsString());
    }

    @Test
    public void CreateCreditAccountPayment_withCaseType() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(jsonRequestWithCaseType().getBytes(), CreditAccountPaymentRequest.class);

        OrganisationalServiceDto organisationalServiceDto = OrganisationalServiceDto.orgServiceDtoWith()
            .serviceCode("AAD7")
            .serviceDescription("Divorce")
            .ccdCaseTypes(Collections.singletonList("DIVORCE"))
            .build();

        when(referenceDataService.getOrganisationalDetail(any(),any(), any())).thenReturn(organisationalServiceDto);

        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getSuccessPaymentAccountResponse());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());


        MvcResult res = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(res.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
        assertEquals("Success", paymentDto.getStatus());
        assertNotNull(paymentDto.getPaymentGroupReference());
    }

    @Test
    public void CreateCreditAccountPayment_withCaseTypereturn404() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(jsonRequestWithCaseType().getBytes(), CreditAccountPaymentRequest.class);

        Mockito.when(referenceDataService.getOrganisationalDetail(any(),any(), any())).thenThrow(new NoServiceFoundException("Test Error"));

        restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isNotFound())
            .andExpect(content().string("Test Error"));
    }

    @Test
    public void CreateCreditAccountPayment_withCaseTypeReturn504() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(jsonRequestWithCaseType().getBytes(), CreditAccountPaymentRequest.class);

        Mockito.when(referenceDataService.getOrganisationalDetail(any(),any(), any())).thenThrow(new GatewayTimeoutException("Test Error"));

        restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isGatewayTimeout())
            .andExpect(content().string("Test Error"));
    }

    @Test
    public void createCreditAccountPayment_withEitherCcdCaseNumberOrCaseReferenceTest() throws Exception {
        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getSuccessPaymentAccountResponse());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());

        MvcResult result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
        assertEquals("Success", paymentDto.getStatus());
    }


    @Test
    public void failCreditAccountPaymentForFinRemAndLiberataRespondsAccountHasInsufficientFundsShouldReturnPaymentFailed() throws Exception {

        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(100), new BigDecimal(100), AccountStatus.ACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);
        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getAnErrorDueToExceededCreditLimitPaymentAccountResponse());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());

        MvcResult result = restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isForbidden()).andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        assertEquals("Failed", paymentDto.getStatus());
        assertEquals("CA-E0001", paymentDto.getStatusHistories().get(0).getErrorCode());
        assertEquals("Exceeded credit limit.", paymentDto.getStatusHistories().get(0).getErrorMessage());
    }

    @Test
    public void failCreditAccountPaymentWhenLiberataRespondsAccountStatusOnHold() throws Exception {
        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getAnErrorDueToAccountOnHold());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());

        MvcResult result = restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isForbidden()).andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        assertEquals("Failed", paymentDto.getStatus());
        assertEquals("CA-E0003", paymentDto.getStatusHistories().get(0).getErrorCode());
        assertEquals("Account not active.", paymentDto.getStatusHistories().get(0).getErrorMessage());
    }

    @Test
    public void failCreditAccountPaymentWhenLiberataRespondsAccountStatusDeleted() throws Exception {
        AccountDto accountDeletedDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(100), new BigDecimal(100), AccountStatus.DELETED, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountDeletedDto);
        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getAnErrorDueToDeleted());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());

        MvcResult result = restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isForbidden()).andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        assertEquals("Failed", paymentDto.getStatus());
        assertEquals("CA-E0004", paymentDto.getStatusHistories().get(0).getErrorCode());
        assertEquals(ACCOUNT_NOT_FOUND.getValue(), paymentDto.getStatusHistories().get(0).getErrorMessage());
    }

    @Test
    public void createCreditAccountPaymentAndLiberataRespondsCannotFindAccountShouldReturn404() throws Exception {
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenThrow(HttpClientErrorException.class);
        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getAnErrorDueToAccountNotFound());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());


        MvcResult result = restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isForbidden()).andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        assertEquals("Failed", paymentDto.getStatus());
        assertEquals("CA-E0004", paymentDto.getStatusHistories().get(0).getErrorCode());
        assertEquals(ACCOUNT_NOT_FOUND.getValue(), paymentDto.getStatusHistories().get(0).getErrorMessage());
    }

    @Test
    public void createCreditAccountPaymentAndLiberataIsNotResponsiveShouldReturn504() throws Exception {

        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenThrow(GatewayTimeoutException.class);
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());

        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isGatewayTimeout());
    }

    @Test
    public void createCreditAccountPaymentWithSuccessWhenAvailableBalanceGreaterThanRequestedAmount() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFinRemJson().getBytes(),
            CreditAccountPaymentRequest.class);
        request.setAmount(new BigDecimal(50));

        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(100), new BigDecimal(100), AccountStatus.ACTIVE, new Date());

        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getSuccessPaymentAccountResponse());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());

        MvcResult result = restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isCreated()).andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        assertEquals("Success", paymentDto.getStatus());
    }

    @Test
    public void createCreditAccountPaymentWithSuccessWhenAvailableBalanceEqualToRequestedAmount() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFinRemJson().getBytes(),
            CreditAccountPaymentRequest.class);
        request.setAmount(new BigDecimal("100.99"));

        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(200), new BigDecimal("100.99"), AccountStatus.ACTIVE, new Date());

        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getSuccessPaymentAccountResponse());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());

        MvcResult result = restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isCreated()).andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        assertEquals("Success", paymentDto.getStatus());
    }

    @Test
    public void createCreditAccountPaymentWithFailedWhenAvailableBalanceGreaterThanRequestedAmount() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFinRemJson().getBytes(),
            CreditAccountPaymentRequest.class);
        request.setAmount(new BigDecimal(101));

        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getAnErrorDueToExceededCreditLimitPaymentAccountResponse());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());


        MvcResult result = restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isForbidden()).andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        assertEquals("Failed", paymentDto.getStatus());
        assertEquals("CA-E0001", paymentDto.getStatusHistories().get(0).getErrorCode());
        assertEquals(EXCEEDED_CREDIT_LIMIT.getValue(), paymentDto.getStatusHistories().get(0).getErrorMessage());
    }

    @Test
    public void givenLiberataCheckOnAndCheckLiberataAccountForAllSericesOnThenAllServicesTriggerLiberataCheck() throws Exception {
        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getSuccessPaymentAccountResponse());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());

        MvcResult result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);

        request = objectMapper.readValue(creditAccountPaymentRequestJsonWithProbateJson().getBytes(), CreditAccountPaymentRequest.class);
        result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
    }

    @Test
    public void givenLiberataCheckOffAndCheckLiberataAccountForAllSericesOffThenNoServiceTriggersLiberataCheck() throws Exception {
        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getSuccessPaymentAccountResponse());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());


        MvcResult result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);

        request = objectMapper.readValue(creditAccountPaymentRequestJsonWithProbateJson().getBytes(), CreditAccountPaymentRequest.class);
        result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
    }

    @Test
    public void givenLiberataCheckOnAndCheckLiberataAccountForAllSericesOffThenOnlyFINREMTriggersLiberataCheck() throws Exception {

        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getSuccessPaymentAccountResponse());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());

        MvcResult result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);

        request = objectMapper.readValue(creditAccountPaymentRequestJsonWithProbateJson().getBytes(), CreditAccountPaymentRequest.class);
        result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
    }

    @Test
    public void givenLiberataCheckOnAndCheckLiberataAccountForAllSericesOffThenOnlyFPLATriggersLiberataCheck() throws Exception {

        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFPLAJson().getBytes(), CreditAccountPaymentRequest.class);
        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getSuccessPaymentAccountResponse());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());

        MvcResult result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);

        request = objectMapper.readValue(creditAccountPaymentRequestJsonWithProbateJson().getBytes(), CreditAccountPaymentRequest.class);
        result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
    }

    @Test
    public void givenLiberataCheckOnAndCheckLiberataAccountForAllSericesOffThenOnlyIACTriggersLiberataCheck() throws Exception {

        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithIACJson().getBytes(), CreditAccountPaymentRequest.class);
        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getSuccessPaymentAccountResponse());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());

        MvcResult result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);

        request = objectMapper.readValue(creditAccountPaymentRequestJsonWithProbateJson().getBytes(), CreditAccountPaymentRequest.class);
        result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
    }

    @Test
    public void givenLiberataCheckOffAndCheckLiberataAccountForAllSericesOnThenAllServicesTriggerLiberataCheck() throws Exception {

        setupForPaymentRoleUser();
        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getSuccessPaymentAccountResponse());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());

        MvcResult result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);

        request = objectMapper.readValue(creditAccountPaymentRequestJsonWithProbateJson().getBytes(), CreditAccountPaymentRequest.class);
        result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);

        // Retrieve payment by payment group reference
        MvcResult result3 = restActions
            .get("/payment-groups/" + paymentDto.getPaymentGroupReference())
            .andExpect(status().isOk())
            .andReturn();

        PaymentGroupDto paymentGroupDto = objectMapper.readValue(result3.getResponse().getContentAsByteArray(), PaymentGroupDto.class);
        PaymentDto paymentDtoForCredit = paymentGroupDto.getPayments().get(0);
        //PAY-2856-Missing PBA details changes
        assertTrue(paymentDtoForCredit.getAccountNumber().equalsIgnoreCase("AC101010"));
        assertTrue(paymentDtoForCredit.getCustomerReference().equalsIgnoreCase("CUST101"));
        assertTrue(paymentDtoForCredit.getOrganisationName().equalsIgnoreCase("ORG101"));

    }

    @Test
    public void givenLiberataCheckOffAndCheckLiberataAccountForAllServicesOnThenAllServicesTriggerLiberataCheck_FPLA() throws Exception {

        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFPLAJson().getBytes(), CreditAccountPaymentRequest.class);
        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getSuccessPaymentAccountResponse());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());

        MvcResult result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);

        request = objectMapper.readValue(creditAccountPaymentRequestJsonWithProbateJson().getBytes(), CreditAccountPaymentRequest.class);
        result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
    }

    @Test
    public void givenLiberataCheckOffAndCheckLiberataAccountForAllServicesOnThenAllServicesTriggerLiberataCheck_IAC() throws Exception {

        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithIAC_Json().getBytes(), CreditAccountPaymentRequest.class);
        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getSuccessPaymentAccountResponse());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());

        MvcResult result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);

        request = objectMapper.readValue(creditAccountPaymentRequestJsonWithProbateJson().getBytes(), CreditAccountPaymentRequest.class);
        result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
    }

    @Test
    public void checkPBAPaymentsFor_Unspec_Service() throws Exception {

        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithUnSpec_Json().getBytes(), CreditAccountPaymentRequest.class);
        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getSuccessPaymentAccountResponse());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());

        MvcResult result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);

        request = objectMapper.readValue(creditAccountPaymentRequestJsonWithProbateJson().getBytes(), CreditAccountPaymentRequest.class);
        result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
    }

    @Test
    public void checkPBAPaymentsErrorWithNoServiceNameFor_Unspec_Service() throws Exception {

        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithNoServiceNameForUnSpec_Json().getBytes(), CreditAccountPaymentRequest.class);
        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);

        MvcResult result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isUnprocessableEntity())
            .andReturn();
    }

    @Test
    public void createCreditAccountPaymentTest_FPLService() throws Exception {

        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFPLJson().getBytes(), CreditAccountPaymentRequest.class);
        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());

        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getSuccessPaymentAccountResponse());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());

        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isCreated());
    }

    @Test
    public void createCreditAccountPaymentTest_ProbateService_NoSiteId() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithNoSiteIdForProbateJson().getBytes(), CreditAccountPaymentRequest.class);
        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);

        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void createCreditAccountPaymentWithMultipleFee_ExactPayment() throws Exception {

        String ccdCaseNumber = "1111CC12" + RandomUtils.nextInt();

        when(featureToggler.getBooleanValue("apportion-feature", false)).thenReturn(true);
        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getSuccessPaymentAccountResponse());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());

        List<FeeDto> fees = new ArrayList<>();
        fees.add(0, FeeDto.feeDtoWith().code("FEE0271").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(20))
            .volume(1).version("1").calculatedAmount(new BigDecimal(20)).build());
        fees.add(1, FeeDto.feeDtoWith().code("FEE0272").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(40))
            .volume(1).version("1").calculatedAmount(new BigDecimal(40)).build());
        fees.add(2, FeeDto.feeDtoWith().code("FEE0273").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(60))
            .volume(1).version("1").calculatedAmount(new BigDecimal(60)).build());

        CreditAccountPaymentRequest request = CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith()
            .amount(new BigDecimal("120"))
            .description("description")
            .caseReference("telRefNumber")
            .ccdCaseNumber(ccdCaseNumber)
            .service("PROBATE")
            .currency(CurrencyCode.GBP)
            .siteId("ABA6")
            .customerReference("CUST101")
            .organisationName("ORG101")
            .accountNumber("AC101010")
            .fees(fees)
            .build();

        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);

        MvcResult result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        List<PaymentFee> mockFees = new ArrayList<>();
        PaymentFee fee1 = PaymentFee.feeWith().amountDue(BigDecimal.valueOf(0)).build();
        PaymentFee fee2 = PaymentFee.feeWith().amountDue(BigDecimal.valueOf(0)).build();
        PaymentFee fee3 = PaymentFee.feeWith().amountDue(BigDecimal.valueOf(0)).build();
        mockFees.add(fee1);
        mockFees.add(fee2);
        mockFees.add(fee3);
        PaymentFeeLink mockFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .fees(mockFees)
            .build();
        PaymentDbBackdoor mockDb = mock(PaymentDbBackdoor.class);
        when(mockDb.findByReference(paymentDto.getPaymentGroupReference())).thenReturn(mockFeeLink);

        List<PaymentFee> savedfees = mockDb.findByReference(paymentDto.getPaymentGroupReference()).getFees();

        assertEquals(new BigDecimal(0), savedfees.get(0).getAmountDue());
        assertEquals(new BigDecimal(0), savedfees.get(1).getAmountDue());
        assertEquals(new BigDecimal(0), savedfees.get(2).getAmountDue());

    }

    @Test
    public void createCreditAccountPaymentWithMultipleFee_ShortfallPayment() throws Exception {

        String ccdCaseNumber = "1111CC12" + RandomUtils.nextInt();

        when(featureToggler.getBooleanValue("apportion-feature", false)).thenReturn(true);
        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getSuccessPaymentAccountResponse());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());

        List<FeeDto> fees = new ArrayList<>();
        fees.add(0, FeeDto.feeDtoWith().code("FEE0271").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(30))
            .volume(1).version("1").calculatedAmount(new BigDecimal(30)).build());
        fees.add(1, FeeDto.feeDtoWith().code("FEE0272").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(40))
            .volume(1).version("1").calculatedAmount(new BigDecimal(40)).build());
        fees.add(2, FeeDto.feeDtoWith().code("FEE0273").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(60))
            .volume(1).version("1").calculatedAmount(new BigDecimal(60)).build());

        CreditAccountPaymentRequest request = CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith()
            .amount(new BigDecimal("120"))
            .description("description")
            .caseReference("telRefNumber")
            .ccdCaseNumber(ccdCaseNumber)
            .service("PROBATE")
            .currency(CurrencyCode.GBP)
            .siteId("ABA6")
            .fees(fees)
            .customerReference("CUST101")
            .organisationName("ORG101")
            .accountNumber("AC101010")
            .build();

        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);

        MvcResult result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        List<PaymentFee> mockFees = new ArrayList<>();
        PaymentFee fee1 = PaymentFee.feeWith().amountDue(BigDecimal.valueOf(0)).build();
        PaymentFee fee2 = PaymentFee.feeWith().amountDue(BigDecimal.valueOf(0)).build();
        PaymentFee fee3 = PaymentFee.feeWith().amountDue(BigDecimal.valueOf(10)).build();
        mockFees.add(fee1);
        mockFees.add(fee2);
        mockFees.add(fee3);
        PaymentFeeLink mockFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .fees(mockFees)
            .build();
        PaymentDbBackdoor mockDb = mock(PaymentDbBackdoor.class);
        when(mockDb.findByReference(paymentDto.getPaymentGroupReference())).thenReturn(mockFeeLink);

        List<PaymentFee> savedfees = mockDb.findByReference(paymentDto.getPaymentGroupReference()).getFees();

        assertEquals(new BigDecimal(0), savedfees.get(0).getAmountDue());
        assertEquals(new BigDecimal(0), savedfees.get(1).getAmountDue());
        assertEquals(new BigDecimal(10), savedfees.get(2).getAmountDue());
    }

    @Test
    public void createCreditAccountPaymentWithMultipleFee_SurplusPayment() throws Exception {

        String ccdCaseNumber = "1111CC12" + RandomUtils.nextInt();

        when(featureToggler.getBooleanValue("apportion-feature", false)).thenReturn(true);
        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getSuccessPaymentAccountResponse());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());

        List<FeeDto> fees = new ArrayList<>();
        fees.add(0, FeeDto.feeDtoWith().code("FEE0271").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(10))
            .volume(1).version("1").calculatedAmount(new BigDecimal(10)).build());
        fees.add(1, FeeDto.feeDtoWith().code("FEE0272").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(40))
            .volume(1).version("1").calculatedAmount(new BigDecimal(40)).build());
        fees.add(2, FeeDto.feeDtoWith().code("FEE0273").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(60))
            .volume(1).version("1").calculatedAmount(new BigDecimal(60)).build());

        CreditAccountPaymentRequest request = CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith()
            .amount(new BigDecimal("120"))
            .description("description")
            .caseReference("telRefNumber")
            .ccdCaseNumber(ccdCaseNumber)
            .service("PROBATE")
            .currency(CurrencyCode.GBP)
            .siteId("ABA6")
            .fees(fees)
            .customerReference("CUST101")
            .organisationName("ORG101")
            .accountNumber("AC101010")
            .build();

        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);

        MvcResult result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        List<PaymentFee> mockFees = new ArrayList<>();
        PaymentFee fee1 = PaymentFee.feeWith().amountDue(BigDecimal.valueOf(0)).build();
        PaymentFee fee2 = PaymentFee.feeWith().amountDue(BigDecimal.valueOf(0)).build();
        PaymentFee fee3 = PaymentFee.feeWith().amountDue(BigDecimal.valueOf(-10)).build();
        mockFees.add(fee1);
        mockFees.add(fee2);
        mockFees.add(fee3);
        PaymentFeeLink mockFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .fees(mockFees)
            .build();
        PaymentDbBackdoor mockDb = mock(PaymentDbBackdoor.class);
        when(mockDb.findByReference(paymentDto.getPaymentGroupReference())).thenReturn(mockFeeLink);

        List<PaymentFee> savedfees = mockDb.findByReference(paymentDto.getPaymentGroupReference()).getFees();

        assertEquals(new BigDecimal(0), savedfees.get(0).getAmountDue());
        assertEquals(new BigDecimal(0), savedfees.get(1).getAmountDue());
        assertEquals(new BigDecimal(-10), savedfees.get(2).getAmountDue());
    }

    @Test
    public void createCreditAccountPaymentWithMultipleFee_AmountDue() throws Exception {

        String ccdCaseNumber = "1111CC12" + RandomUtils.nextInt();

        when(featureToggler.getBooleanValue("apportion-feature", false)).thenReturn(true);
        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getSuccessPaymentAccountResponse());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());

        List<FeeDto> fees = new ArrayList<>();
        fees.add(0, FeeDto.feeDtoWith().code("FEE0271").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(20))
            .volume(1).version("1").calculatedAmount(new BigDecimal(20)).build());
        fees.add(1, FeeDto.feeDtoWith().code("FEE0272").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(40))
            .volume(1).version("1").calculatedAmount(new BigDecimal(40)).build());
        fees.add(2, FeeDto.feeDtoWith().code("FEE0273").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(60))
            .volume(1).version("1").calculatedAmount(new BigDecimal(60)).build());

        CreditAccountPaymentRequest request = CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith()
            .amount(new BigDecimal("100"))
            .description("description")
            .caseReference("telRefNumber")
            .ccdCaseNumber(ccdCaseNumber)
            .service("FPL")
            .currency(CurrencyCode.GBP)
            .siteId("ABA3")
            .customerReference("CUST101")
            .organisationName("ORG101")
            .accountNumber("AC101010")
            .fees(fees)
            .build();

        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);

        MvcResult result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        List<PaymentFee> mockFees = new ArrayList<>();
        PaymentFee fee1 = PaymentFee.feeWith().amountDue(BigDecimal.valueOf(0)).build();
        PaymentFee fee2 = PaymentFee.feeWith().amountDue(BigDecimal.valueOf(0)).build();
        PaymentFee fee3 = PaymentFee.feeWith().amountDue(BigDecimal.valueOf(20)).build();
        mockFees.add(fee1);
        mockFees.add(fee2);
        mockFees.add(fee3);
        PaymentFeeLink mockFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .fees(mockFees)
            .build();
        PaymentDbBackdoor mockDb = mock(PaymentDbBackdoor.class);
        when(mockDb.findByReference(paymentDto.getPaymentGroupReference())).thenReturn(mockFeeLink);

        List<PaymentFee> savedfees = mockDb.findByReference(paymentDto.getPaymentGroupReference()).getFees();

        assertEquals(new BigDecimal(0), savedfees.get(0).getAmountDue());
        assertEquals(new BigDecimal(0), savedfees.get(1).getAmountDue());
        assertEquals(new BigDecimal(20), savedfees.get(2).getAmountDue());
    }

    @Test
    public void createCreditAccountPaymentWithMultipleFee_AmountDue_When_Apportion_Flag_Is_True() throws Exception {

        String ccdCaseNumber = "1111CC12" + RandomUtils.nextInt();
        when(featureToggler.getBooleanValue("apportion-feature", false)).thenReturn(true);
        Mockito.when(liberataRealTimeAPI.payByAccount(any())).thenReturn(getSuccessPaymentAccountResponse());
        Mockito.when(feesService.getFeeVersion(any(),any())).thenReturn(getFeeVersionDto());
        Mockito.when(feesService.getFeesDtoMap()).thenReturn(getFeesDtoMap());

        List<FeeDto> fees = new ArrayList<>();
        fees.add(0, FeeDto.feeDtoWith().code("FEE0271").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(20))
            .volume(1).version("1").calculatedAmount(new BigDecimal(20)).build());
        fees.add(1, FeeDto.feeDtoWith().code("FEE0272").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(40))
            .volume(1).version("1").calculatedAmount(new BigDecimal(40)).build());
        fees.add(2, FeeDto.feeDtoWith().code("FEE0273").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(60))
            .volume(1).version("1").calculatedAmount(new BigDecimal(60)).build());

        CreditAccountPaymentRequest request = CreditAccountPaymentRequest.createCreditAccountPaymentRequestDtoWith()
            .amount(new BigDecimal("100"))
            .description("description")
            .caseReference("telRefNumber")
            .ccdCaseNumber(ccdCaseNumber)
            .service("FPL")
            .currency(CurrencyCode.GBP)
            .siteId("ABA3")
            .customerReference("CUST101")
            .organisationName("ORG101")
            .accountNumber("AC101010")
            .fees(fees)
            .build();

        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);

        MvcResult result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        List<PaymentFee> mockFees = new ArrayList<>();
        PaymentFee fee1 = PaymentFee.feeWith().amountDue(BigDecimal.valueOf(0)).build();
        PaymentFee fee2 = PaymentFee.feeWith().amountDue(BigDecimal.valueOf(0)).build();
        PaymentFee fee3 = PaymentFee.feeWith().amountDue(BigDecimal.valueOf(20)).build();
        mockFees.add(fee1);
        mockFees.add(fee2);
        mockFees.add(fee3);
        PaymentFeeLink mockFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .fees(mockFees)
            .build();
        PaymentDbBackdoor mockDb = mock(PaymentDbBackdoor.class);
        when(mockDb.findByReference(paymentDto.getPaymentGroupReference())).thenReturn(mockFeeLink);

        List<PaymentFee> savedfees = mockDb.findByReference(paymentDto.getPaymentGroupReference()).getFees();

        assertEquals(new BigDecimal(0), savedfees.get(0).getAmountDue());
        assertEquals(new BigDecimal(0), savedfees.get(1).getAmountDue());
        assertEquals(new BigDecimal(20), savedfees.get(2).getAmountDue());
    }

    @Test
    public void testDeletePayment() throws Exception {
        restActions.delete("/credit-account-payments/test")
            .andExpect(status().isNotFound())
            .andReturn();
    }

    private TokenResponse createTestTokenResponse() {
        long createdAt = System.currentTimeMillis() / 1000L;
        long expiresIn = createdAt + 3600L; // expires in 1 hour
        return new TokenResponse("", expiresIn, createdAt);
    }

    private PaymentAccountResponse getSuccessPaymentAccountResponse() {
        return PaymentAccountResponse.paymentDtoWith()
            .status(PaymentAccountResponseStatus.SUCCESS.getValue())
            .message(PaymentAccountResponseStatus.SUCCESSFULLY.getValue()).build();
    }

    private PaymentAccountResponse getAnErrorDueToExceededCreditLimitPaymentAccountResponse() {
        return PaymentAccountResponse.paymentDtoWith()
            .status(PaymentAccountResponseStatus.ERROR.getValue())
            .message(EXCEEDED_CREDIT_LIMIT.getValue()).build();
    }

    private PaymentAccountResponse getAnErrorDueToAccountOnHold() {
        return PaymentAccountResponse.paymentDtoWith()
            .status(PaymentAccountResponseStatus.ERROR.getValue())
            .message(PaymentAccountResponseStatus.ACCOUNT_NOT_ACTIVE.getValue()).build();
    }

    public Optional<FeeVersionDto> getFeeVersionDto() {
        val feeVersionDto = new FeeVersionDto();
        // set required fee code and version
        feeVersionDto.setVersion(1);
        // set other attributes with dummy values so getters can be accessed
        feeVersionDto.setMemoLine("Dummy memo line");
        feeVersionDto.setNaturalAccountCode("NAT-ACCT-001");
        // return the populated object
        return Optional.of(feeVersionDto);
    }

    public Map<String, Fee2Dto> getFeesDtoMap() {
        Map<String, Fee2Dto> feesMap = new HashMap<>();
        val  fee2Dto = new Fee2Dto();
        fee2Dto.setCode("X0101");

        val jurisdiction1 = new Jurisdiction1Dto();
        jurisdiction1.setName("JURISDICTION_ONE");
        fee2Dto.setJurisdiction1Dto(jurisdiction1);

        val jurisdiction2 = new Jurisdiction2Dto();
        jurisdiction2.setName("JURISDICTION_TWO");
        fee2Dto.setJurisdiction2Dto(jurisdiction2);
        feesMap.put("X0101", fee2Dto);

        val  fee2Dto1 = new Fee2Dto();
        fee2Dto1.setCode("FEE0271");

        val jurisdiction11 = new Jurisdiction1Dto();
        jurisdiction11.setName("JURISDICTION_ONE");
        fee2Dto1.setJurisdiction1Dto(jurisdiction11);

        val jurisdiction21 = new Jurisdiction2Dto();
        jurisdiction21.setName("JURISDICTION_TWO");
        fee2Dto1.setJurisdiction2Dto(jurisdiction21);
        feesMap.put("FEE0271", fee2Dto1);

        val  fee2Dto2 = new Fee2Dto();
        fee2Dto2.setCode("FEE0272");

        val jurisdiction12 = new Jurisdiction1Dto();
        jurisdiction12.setName("JURISDICTION_ONE");
        fee2Dto2.setJurisdiction1Dto(jurisdiction12);

        val jurisdiction22 = new Jurisdiction2Dto();
        jurisdiction22.setName("JURISDICTION_TWO");
        fee2Dto2.setJurisdiction2Dto(jurisdiction22);
        feesMap.put("FEE0272", fee2Dto2);

        val  fee2Dto3 = new Fee2Dto();
        fee2Dto3.setCode("FEE0273");

        val jurisdiction13 = new Jurisdiction1Dto();
        jurisdiction13.setName("JURISDICTION_ONE");
        fee2Dto3.setJurisdiction1Dto(jurisdiction13);

        val jurisdiction23 = new Jurisdiction2Dto();
        jurisdiction23.setName("JURISDICTION_TWO");
        fee2Dto3.setJurisdiction2Dto(jurisdiction23);
        feesMap.put("FEE0273", fee2Dto3);

        val  fee2Dto4 = new Fee2Dto();
        fee2Dto4.setCode("X0102");

        val jurisdiction14 = new Jurisdiction1Dto();
        jurisdiction14.setName("JURISDICTION_ONE");
        fee2Dto4.setJurisdiction1Dto(jurisdiction14);

        val jurisdiction24 = new Jurisdiction2Dto();
        jurisdiction24.setName("JURISDICTION_TWO");
        fee2Dto4.setJurisdiction2Dto(jurisdiction24);
        feesMap.put("X0102", fee2Dto4);

        return feesMap;
    }




    private PaymentAccountResponse getAnErrorDueToDeleted() {
        return PaymentAccountResponse.paymentDtoWith()
            .status(PaymentAccountResponseStatus.ERROR.getValue())
            .message(ACCOUNT_NOT_FOUND.getValue()).build();
    }

    private PaymentAccountResponse getAnErrorDueToAccountNotFound() {
        return PaymentAccountResponse.paymentDtoWith()
            .status(PaymentAccountResponseStatus.ERROR.getValue())
            .message(ACCOUNT_NOT_FOUND.getValue()).build();
    }

    private String jsonRequestWithoutCcdCaseRefAndCaseRef() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"service\": \"PROBATE\",\n" +
            "  \"currency\": \"GBP\",\n" +
            "  \"site_id\": \"ABA6\",\n" +
            "  \"customer_reference\": \"CUST101\",\n" +
            "  \"organisation_name\": \"ORG101\",\n" +
            "  \"account_number\": \"AC101010\",\n" +
            "  \"fees\": [\n" +
            "    {\n" +
            "      \"calculated_amount\": 101.89,\n" +
            "      \"code\": \"X0101\",\n" +
            "      \"volume\": 1,\n" +
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private String jsonRequestWithoutSiteIdAndCaseType() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"ccd_case_number\": \"CCD101\",\n" +
            "  \"case_reference\": \"12345\",\n" +
            "  \"service\": \"PROBATE\",\n" +
            "  \"currency\": \"GBP\",\n" +
            "  \"customer_reference\": \"CUST101\",\n" +
            "  \"organisation_name\": \"ORG101\",\n" +
            "  \"account_number\": \"AC101010\",\n" +
            "  \"fees\": [\n" +
            "    {\n" +
            "      \"calculated_amount\": 101.89,\n" +
            "      \"code\": \"X0101\",\n" +
            "      \"volume\": 1,\n" +
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private String jsonRequestWithCaseType() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"case_type\": \"Caveat\",\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"ccd_case_number\": \"CCD102\",\n" +
            "  \"case_reference\": \"12345\",\n" +
            "  \"service\": \"DIVORCE\",\n" +
            "  \"currency\": \"GBP\",\n" +
            "  \"customer_reference\": \"CUST101\",\n" +
            "  \"organisation_name\": \"ORG101\",\n" +
            "  \"account_number\": \"AC101010\",\n" +
            "  \"fees\": [\n" +
            "    {\n" +
            "      \"calculated_amount\": 101.89,\n" +
            "      \"code\": \"X0101\",\n" +
            "      \"volume\": 1,\n" +
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private String jsonRequestWithCaseReference() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"case_reference\": \"caseReference\",\n" +
            "  \"service\": \"PROBATE\",\n" +
            "  \"currency\": \"GBP\",\n" +
            "  \"site_id\": \"AA101\",\n" +
            "  \"customer_reference\": \"CUST101\",\n" +
            "  \"organisation_name\": \"ORG101\",\n" +
            "  \"account_number\": \"AC101010\",\n" +
            "  \"fees\": [\n" +
            "    {\n" +
            "      \"calculated_amount\": 101.89,\n" +
            "      \"code\": \"X0101\",\n" +
            "      \"volume\": 1,\n" +
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private String creditAccountPaymentRequestJson() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"ccd_case_number\": \"CCD103\",\n" +
            "  \"case_reference\": \"12345\",\n" +
            "  \"service\": \"PROBATE\",\n" +
            "  \"currency\": \"GBP\",\n" +
            "  \"site_id\": \"ABA6\",\n" +
            "  \"customer_reference\": \"CUST101\",\n" +
            "  \"organisation_name\": \"ORG101\",\n" +
            "  \"account_number\": \"AC101010\",\n" +
            "  \"fees\": [\n" +
            "    {\n" +
            "      \"calculated_amount\": 101.89,\n" +
            "      \"code\": \"X0101\",\n" +
            "      \"volume\": 1,\n" +
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private String creditAccountPaymentRequestJsonWithFinRemJson() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"ccd_case_number\": \"CCD104\",\n" +
            "  \"case_reference\": \"12345\",\n" +
            "  \"service\": \"FINREM\",\n" +
            "  \"currency\": \"GBP\",\n" +
            "  \"site_id\": \"AA101\",\n" +
            "  \"customer_reference\": \"CUST101\",\n" +
            "  \"organisation_name\": \"ORG101\",\n" +
            "  \"account_number\": \"AC101010\",\n" +
            "  \"fees\": [\n" +
            "    {\n" +
            "      \"calculated_amount\": 101.89,\n" +
            "      \"code\": \"X0101\",\n" +
            "      \"volume\": 1,\n" +
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private String creditAccountPaymentRequestJsonWithProbateJson() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"ccd_case_number\": \"CCD105\",\n" +
            "  \"case_reference\": \"12345\",\n" +
            "  \"service\": \"PROBATE\",\n" +
            "  \"currency\": \"GBP\",\n" +
            "  \"site_id\": \"ABA6\",\n" +
            "  \"customer_reference\": \"CUST101\",\n" +
            "  \"organisation_name\": \"ORG101\",\n" +
            "  \"account_number\": \"AC101010\",\n" +
            "  \"fees\": [\n" +
            "    {\n" +
            "      \"calculated_amount\": 101.89,\n" +
            "      \"code\": \"X0101\",\n" +
            "      \"volume\": 1,\n" +
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private String creditAccountPaymentRequestJsonWithInvalidSiteIdForProbateJson() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"ccd_case_number\": \"CCD106\",\n" +
            "  \"case_reference\": \"12345\",\n" +
            "  \"service\": \"PROBATE\",\n" +
            "  \"currency\": \"GBP\",\n" +
            "  \"site_id\": \"AA00\",\n" +
            "  \"customer_reference\": \"CUST101\",\n" +
            "  \"organisation_name\": \"ORG101\",\n" +
            "  \"account_number\": \"AC101010\",\n" +
            "  \"fees\": [\n" +
            "    {\n" +
            "      \"calculated_amount\": 101.89,\n" +
            "      \"code\": \"X0101\",\n" +
            "      \"volume\": 1,\n" +
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private String creditAccountPaymentRequestJsonWithNoSiteIdForProbateJson() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"ccd_case_number\": \"CCD107\",\n" +
            "  \"case_reference\": \"12345\",\n" +
            "  \"service\": \"PROBATE\",\n" +
            "  \"currency\": \"GBP\",\n" +
            "  \"customer_reference\": \"CUST101\",\n" +
            "  \"organisation_name\": \"ORG101\",\n" +
            "  \"account_number\": \"AC101010\",\n" +
            "  \"fees\": [\n" +
            "    {\n" +
            "      \"calculated_amount\": 101.89,\n" +
            "      \"code\": \"X0101\",\n" +
            "      \"volume\": 1,\n" +
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private String creditAccountPaymentInvalidRequestJson() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"ccd_case_number\": \"CCD108\",\n" +
            "  \"case_reference\": \"12345\",\n" +
            "  \"service\": \"PROBATE\",\n" +
            "  \"currency\": \"GBP\",\n" +
            "  \"site_id\": \"AA101\",\n" +
            "  \"customer_reference\": \"CUST101\",\n" +
            "  \"organisation_name\": \"ORG101\",\n" +
            "  \"account_number\": \"AC101010\",\n" +
            "  \"fees\": [\n" +
            "  ]\n" +
            "}";
    }


    private String creditAccountPaymentRequestJsonWithInvalidServiceName() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"ccd_case_number\": \"CCD109\",\n" +
            "  \"case_reference\": \"12345\",\n" +
            "  \"service\": \"NO_SERVICE\",\n" +
            "  \"currency\": \"GBP\",\n" +
            "  \"site_id\": \"AA101\",\n" +
            "  \"customer_reference\": \"CUST101\",\n" +
            "  \"organisation_name\": \"ORG101\",\n" +
            "  \"account_number\": \"AC101010\",\n" +
            "  \"fees\": [\n" +
            "    {\n" +
            "      \"calculated_amount\": 101.89,\n" +
            "      \"code\": \"X0101\",\n" +
            "      \"volume\": 1,\n" +
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private String creditAccountPaymentRequestJsonWithFPLJson() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"ccd_case_number\": \"CCD110\",\n" +
            "  \"case_reference\": \"12345\",\n" +
            "  \"service\": \"FPL\",\n" +
            "  \"currency\": \"GBP\",\n" +
            "  \"site_id\": \"ABA3\",\n" +
            "  \"customer_reference\": \"CUST101\",\n" +
            "  \"organisation_name\": \"ORG101\",\n" +
            "  \"account_number\": \"AC101010\",\n" +
            "  \"fees\": [\n" +
            "    {\n" +
            "      \"calculated_amount\": 101.89,\n" +
            "      \"code\": \"X0101\",\n" +
            "      \"volume\": 1,\n" +
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private String creditAccountPaymentRequestJsonWithFPLJsonInvalidSiteId() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"ccd_case_number\": \"CCD111\",\n" +
            "  \"case_reference\": \"12345\",\n" +
            "  \"service\": \"FPL\",\n" +
            "  \"currency\": \"GBP\",\n" +
            "  \"site_id\": \"AA07\",\n" +
            "  \"customer_reference\": \"CUST101\",\n" +
            "  \"organisation_name\": \"ORG101\",\n" +
            "  \"account_number\": \"AC101010\",\n" +
            "  \"fees\": [\n" +
            "    {\n" +
            "      \"calculated_amount\": 101.89,\n" +
            "      \"code\": \"X0101\",\n" +
            "      \"volume\": 1,\n" +
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private String creditAccountPaymentRequestJsonWithIACLJsonInvalidSiteId() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"ccd_case_number\": \"CCD112\",\n" +
            "  \"case_reference\": \"12345\",\n" +
            "  \"service\": \"IAC\",\n" +
            "  \"currency\": \"GBP\",\n" +
            "  \"site_id\": \"BFA0\",\n" +
            "  \"customer_reference\": \"CUST101\",\n" +
            "  \"organisation_name\": \"ORG101\",\n" +
            "  \"account_number\": \"AC101010\",\n" +
            "  \"fees\": [\n" +
            "    {\n" +
            "      \"calculated_amount\": 101.89,\n" +
            "      \"code\": \"X0101\",\n" +
            "      \"volume\": 1,\n" +
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private String creditAccountPaymentRequestJsonWithFPLAJson() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"ccd_case_number\": \"CCD113\",\n" +
            "  \"case_reference\": \"12345\",\n" +
            "  \"service\": \"FPL\",\n" +
            "  \"currency\": \"GBP\",\n" +
            "  \"site_id\": \"ABA3\",\n" +
            "  \"customer_reference\": \"CUST101\",\n" +
            "  \"organisation_name\": \"ORG101\",\n" +
            "  \"account_number\": \"AC101010\",\n" +
            "  \"fees\": [\n" +
            "    {\n" +
            "      \"calculated_amount\": 101.89,\n" +
            "      \"code\": \"X0101\",\n" +
            "      \"volume\": 1,\n" +
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }


    private String creditAccountPaymentRequestJsonWithIAC_Json() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"ccd_case_number\": \"CCD114\",\n" +
            "  \"case_reference\": \"12345\",\n" +
            "  \"service\": \"IAC\",\n" +
            "  \"currency\": \"GBP\",\n" +
            "  \"site_id\": \"BFA1\",\n" +
            "  \"customer_reference\": \"CUST101\",\n" +
            "  \"organisation_name\": \"ORG101\",\n" +
            "  \"account_number\": \"AC101010\",\n" +
            "  \"fees\": [\n" +
            "    {\n" +
            "      \"calculated_amount\": 101.89,\n" +
            "      \"code\": \"X0101\",\n" +
            "      \"volume\": 1,\n" +
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private String creditAccountPaymentRequestJsonWithUnSpec_Json() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"ccd_case_number\": \"CCD115\",\n" +
            "  \"case_reference\": \"12345\",\n" +
            "  \"service\": \"UNSPEC\",\n" +
            "  \"currency\": \"GBP\",\n" +
            "  \"site_id\": \"AAA7\",\n" +
            "  \"customer_reference\": \"CUST101\",\n" +
            "  \"organisation_name\": \"ORG101\",\n" +
            "  \"account_number\": \"AC101010\",\n" +
            "  \"fees\": [\n" +
            "    {\n" +
            "      \"calculated_amount\": 101.89,\n" +
            "      \"code\": \"X0101\",\n" +
            "      \"volume\": 1,\n" +
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private String creditAccountPaymentRequestJsonWithDifferentSiteIdForUnSpec_Json() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"ccd_case_number\": \"CCD116\",\n" +
            "  \"case_reference\": \"12345\",\n" +
            "  \"service\": \"UNSPEC\",\n" +
            "  \"currency\": \"GBP\",\n" +
            "  \"site_id\": \"A000\",\n" +
            "  \"customer_reference\": \"CUST101\",\n" +
            "  \"organisation_name\": \"ORG101\",\n" +
            "  \"account_number\": \"AC101010\",\n" +
            "  \"fees\": [\n" +
            "    {\n" +
            "      \"calculated_amount\": 101.89,\n" +
            "      \"code\": \"X0101\",\n" +
            "      \"volume\": 1,\n" +
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private String creditAccountPaymentRequestJsonWithNoServiceNameForUnSpec_Json() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"ccd_case_number\": \"CCD117\",\n" +
            "  \"case_reference\": \"12345\",\n" +
            "  \"currency\": \"GBP\",\n" +
            "  \"site_id\": \"A000\",\n" +
            "  \"customer_reference\": \"CUST101\",\n" +
            "  \"organisation_name\": \"ORG101\",\n" +
            "  \"account_number\": \"AC101010\",\n" +
            "  \"fees\": [\n" +
            "    {\n" +
            "      \"calculated_amount\": 101.89,\n" +
            "      \"code\": \"X0101\",\n" +
            "      \"volume\": 1,\n" +
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private String creditAccountPaymentRequestJsonWithIACJson() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"ccd_case_number\": \"CCD118\",\n" +
            "  \"case_reference\": \"12345\",\n" +
            "  \"service\": \"IAC\",\n" +
            "  \"currency\": \"GBP\",\n" +
            "  \"site_id\": \"BFA1\",\n" +
            "  \"customer_reference\": \"CUST101\",\n" +
            "  \"organisation_name\": \"ORG101\",\n" +
            "  \"account_number\": \"AC101010\",\n" +
            "  \"fees\": [\n" +
            "    {\n" +
            "      \"calculated_amount\": 101.89,\n" +
            "      \"code\": \"X0101\",\n" +
            "      \"volume\": 1,\n" +
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }
}
