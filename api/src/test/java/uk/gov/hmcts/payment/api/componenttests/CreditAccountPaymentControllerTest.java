package uk.gov.hmcts.payment.api.componenttests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.configuration.SecurityUtils;
import uk.gov.hmcts.payment.api.configuration.security.ServiceAndUserAuthFilter;
import uk.gov.hmcts.payment.api.configuration.security.ServicePaymentFilter;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.exception.AccountServiceUnavailableException;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.service.AccountService;
import uk.gov.hmcts.payment.api.util.AccountStatus;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.CustomResultMatcher;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;

import static java.lang.String.format;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static uk.gov.hmcts.payment.api.configuration.security.ServiceAndUserAuthFilterTest.getUserInfoBasedOnUID_Roles;
import static uk.gov.hmcts.payment.api.model.PaymentFeeLink.paymentFeeLinkWith;

@RunWith(SpringRunner.class)
@ActiveProfiles({"componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@Transactional
@EnableFeignClients
@AutoConfigureMockMvc
public class CreditAccountPaymentControllerTest extends PaymentsDataUtil {

    private final static String PAYMENT_REFERENCE_REFEX = "^[RC-]{3}(\\w{4}-){3}(\\w{4})";

    @Autowired
    private ConfigurableListableBeanFactory configurableListableBeanFactory;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    protected PaymentDbBackdoor db;

    @Autowired
    protected Payment2Repository paymentRepository;

    @MockBean
    protected AccountService<AccountDto, String> accountService;

    RestActions restActions;

    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Autowired
    ServiceAuthFilter serviceAuthFilter;

    @Autowired
    ServicePaymentFilter servicePaymentFilter;

    @InjectMocks
    ServiceAndUserAuthFilter serviceAndUserAuthFilter;

    @MockBean
    SecurityUtils securityUtils;

    @Autowired
    private ObjectMapper objectMapper;

    protected CustomResultMatcher body() {
        return new CustomResultMatcher(objectMapper);
    }

    @Before
    public void setup() {
        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, objectMapper);
        when(securityUtils.getUserInfo()).thenReturn(getUserInfoBasedOnUID_Roles("UID123","payments"));
        restActions
            .withAuthorizedService("divorce")
            .withReturnUrl("https://www.gooooogle.com");

        Mockito.reset(accountService);
    }


    @Test
    @WithMockUser(authorities = "payments")
    public void createCreditAccountPaymentTest() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFinRemJson().getBytes(), CreditAccountPaymentRequest.class);
        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);

        setCreditAccountPaymentLiberataCheckFeature(true);

        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void rejectDuplicatePayment_ccdCaseNumber() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFinRemJson().getBytes(), CreditAccountPaymentRequest.class);

        request.setCcdCaseNumber("CCD105");
        request.setCaseReference(null);
        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);

        setCreditAccountPaymentLiberataCheckFeature(true);

        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isCreated());

        // 2nd request
        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void rejectDuplicatePayment_caseReference() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFinRemJson().getBytes(), CreditAccountPaymentRequest.class);

        request.setCcdCaseNumber(null);
        request.setCaseReference("33333");

        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);

        setCreditAccountPaymentLiberataCheckFeature(true);

        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isCreated());

        // 2nd request
        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void shouldNotRejectDuplicatePaymentIfAmountIsDifferent() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFinRemJson().getBytes(), CreditAccountPaymentRequest.class);

        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);

        setCreditAccountPaymentLiberataCheckFeature(true);

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
    @WithMockUser(authorities = "payments")
    public void shouldNotRejectDuplicatePaymentIfFeeCodeIsDifferent() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFinRemJson().getBytes(), CreditAccountPaymentRequest.class);

        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);

        setCreditAccountPaymentLiberataCheckFeature(true);

        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isCreated());

        // different fee code for the 2nd request
        FeeDto x0102 = FeeDto.feeDtoWith().code("X0102").version("1").calculatedAmount(BigDecimal.valueOf(101.89)).build();
        request.setFees(Lists.newArrayList(x0102));
        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void shouldNotRejectDuplicatePaymentIfFeeVersionIsDifferent() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFinRemJson().getBytes(), CreditAccountPaymentRequest.class);

        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName", new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);
        setCreditAccountPaymentLiberataCheckFeature(true);

        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isCreated());

        // different fee version for the 2nd request
        FeeDto x0101_v4 = FeeDto.feeDtoWith().code("X0101").version("4").calculatedAmount(BigDecimal.valueOf(101.89)).build();
        request.setFees(Lists.newArrayList(x0101_v4));
        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "payments")
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
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .reference("RC-1519-9028-1909-3475")
            .build();
        PaymentFee fee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("11.99")).version("1").code("X0001").build();

        PaymentFeeLink paymentFeeLink = db.create(paymentFeeLinkWith().paymentReference("2018-15186162020").payments(Arrays.asList(payment)).fees(Arrays.asList(fee)));
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
    @WithMockUser(authorities = "payments")
    public void validCreditAccountPaymentRequestJsonTest() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJson().getBytes(), CreditAccountPaymentRequest.class);
        assertNotNull(request);
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void createCreditAccountPayment_withInvalidRequestJsonTest() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentInvalidRequestJson().getBytes(), CreditAccountPaymentRequest.class);

        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void createCreditAccountPayment_WithInvalidServiceNameTest() throws Exception {

        restActions
            .post(format("/credit-account-payments"), creditAccountPaymentRequestJsonWithInvalidServiceName())
            .andExpect(status().isBadRequest())
            .andReturn();

    }

    @Test
    @WithMockUser(authorities = "payments")
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
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .reference("RC-1519-9239-1920-0375")
            .statusHistories(Arrays.asList(StatusHistory.statusHistoryWith()
                .status("pending")
                .build()))
            .build();
        PaymentFee fee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("11.99")).version("1").code("X0001").build();

        PaymentFeeLink paymentFeeLink = db.create(paymentFeeLinkWith().paymentReference("2018-15199028243").payments(Arrays.asList(payment)).fees(Arrays.asList(fee)));
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
    @WithMockUser(authorities = "payments")
    public void validateCreateCreditAccountPayment_withoutCcdCaseNumberAndCaseReferenceTest() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(jsonRequestWithoutCcdCaseRefAndCaseRef().getBytes(), CreditAccountPaymentRequest.class);

        MvcResult res = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        assertEquals("eitherOneRequired: Either ccdCaseNumber or caseReference is required.", res.getResponse().getContentAsString());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void createCreditAccountPayment_withEitherCcdCaseNumberOrCaseReferenceTest() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFinRemJson().getBytes(), CreditAccountPaymentRequest.class);
        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);

        setCreditAccountPaymentLiberataCheckFeature(true);

        MvcResult result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
        assertEquals("Success", paymentDto.getStatus());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void failCreditAccountPaymentForFinRemAndLiberataRespondsAccountHasInsufficientFundsShouldReturnPaymentFailed() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFinRemJson().getBytes(), CreditAccountPaymentRequest.class);
        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(100), new BigDecimal(100), AccountStatus.ACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);

        setCreditAccountPaymentLiberataCheckFeature(true);

        MvcResult result = restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isForbidden()).andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        assertEquals("Failed", paymentDto.getStatus());
        assertEquals("CA-E0001", paymentDto.getStatusHistories().get(0).getErrorCode());
        assertEquals("Payment request failed. PBA account accountName have insufficient funds available", paymentDto.getStatusHistories().get(0).getErrorMessage());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void failCreditAccountPaymentWhenLiberataRespondsAccountStatusOnHold() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFinRemJson().getBytes(), CreditAccountPaymentRequest.class);
        AccountDto accountOnHoldDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ON_HOLD, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountOnHoldDto);

        setCreditAccountPaymentLiberataCheckFeature(true);

        MvcResult result = restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isForbidden()).andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        assertEquals("Failed", paymentDto.getStatus());
        assertEquals("CA-E0003", paymentDto.getStatusHistories().get(0).getErrorCode());
        assertEquals("Your account is on hold", paymentDto.getStatusHistories().get(0).getErrorMessage());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void failCreditAccountPaymentWhenLiberataRespondsAccountStatusDeleted() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFinRemJson().getBytes(), CreditAccountPaymentRequest.class);
        AccountDto accountDeletedDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(100), new BigDecimal(100), AccountStatus.DELETED, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountDeletedDto);

        setCreditAccountPaymentLiberataCheckFeature(true);

        MvcResult result = restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isForbidden()).andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        assertEquals("Failed", paymentDto.getStatus());
        assertEquals("CA-E0004", paymentDto.getStatusHistories().get(0).getErrorCode());
        assertEquals("Your account is deleted", paymentDto.getStatusHistories().get(0).getErrorMessage());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void createCreditAccountPaymentAndLiberataRespondsCannotFindAccountShouldReturn404() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFinRemJson().getBytes(), CreditAccountPaymentRequest.class);
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenThrow(HttpClientErrorException.class);

        setCreditAccountPaymentLiberataCheckFeature(true);

        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void createCreditAccountPaymentAndLiberataIsNotResponsiveShouldReturn504() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFinRemJson().getBytes(), CreditAccountPaymentRequest.class);
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenThrow(AccountServiceUnavailableException.class);

        setCreditAccountPaymentLiberataCheckFeature(true);

        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isGatewayTimeout());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void createCreditAccountPaymentWithLiberataFeatureOffShouldReturnPaymentPending() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJson().getBytes(),
            CreditAccountPaymentRequest.class);
        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(100), new BigDecimal(100), AccountStatus.ACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);

        setCreditAccountPaymentLiberataCheckFeature(false);

        MvcResult result = restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isCreated()).andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        assertEquals("Pending", paymentDto.getStatus());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void createCreditAccountPaymentWithSuccessWhenAvailableBalanceGreaterThanRequestedAmount() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFinRemJson().getBytes(),
            CreditAccountPaymentRequest.class);
        request.setAmount(new BigDecimal(50));

        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(100), new BigDecimal(100), AccountStatus.ACTIVE, new Date());

        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);

        setCreditAccountPaymentLiberataCheckFeature(true);

        MvcResult result = restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isCreated()).andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        assertEquals("Success", paymentDto.getStatus());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void createCreditAccountPaymentWithSuccessWhenAvailableBalanceEqualToRequestedAmount() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFinRemJson().getBytes(),
            CreditAccountPaymentRequest.class);
        request.setAmount(new BigDecimal("100.99"));

        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(200), new BigDecimal("100.99"), AccountStatus.ACTIVE, new Date());

        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);

        setCreditAccountPaymentLiberataCheckFeature(true);

        MvcResult result = restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isCreated()).andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        assertEquals("Success", paymentDto.getStatus());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void createCreditAccountPaymentWithFailedWhenAvailableBalanceGreaterThanRequestedAmount() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFinRemJson().getBytes(),
            CreditAccountPaymentRequest.class);
        request.setAmount(new BigDecimal(101));

        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(100), new BigDecimal("100.99"), AccountStatus.ACTIVE, new Date());

        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);

        setCreditAccountPaymentLiberataCheckFeature(true);

        MvcResult result = restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isForbidden()).andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        assertEquals("Failed", paymentDto.getStatus());
        assertEquals("CA-E0001", paymentDto.getStatusHistories().get(0).getErrorCode());
        assertEquals("Payment request failed. PBA account accountName have insufficient funds available", paymentDto.getStatusHistories().get(0).getErrorMessage());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void givenLiberataCheckOnAndCheckLiberataAccountForAllSericesOnThenAllServicesTriggerLiberataCheck() throws Exception {
        setCreditAccountPaymentLiberataCheckFeature(true);
        setCheckLiberataAccountForAllServices(true);
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFinRemJson().getBytes(), CreditAccountPaymentRequest.class);
        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);

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
        verify(accountService, times(2)).retrieve(request.getAccountNumber());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void givenLiberataCheckOffAndCheckLiberataAccountForAllSericesOffThenNoServiceTriggersLiberataCheck() throws Exception {
        setCreditAccountPaymentLiberataCheckFeature(false);
        setCheckLiberataAccountForAllServices(false);
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFinRemJson().getBytes(), CreditAccountPaymentRequest.class);
        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);

        MvcResult result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
        verify(accountService, times(0)).retrieve(request.getAccountNumber());

        request = objectMapper.readValue(creditAccountPaymentRequestJsonWithProbateJson().getBytes(), CreditAccountPaymentRequest.class);
        result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
        verify(accountService, times(0)).retrieve(request.getAccountNumber());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void givenLiberataCheckOnAndCheckLiberataAccountForAllSericesOffThenOnlyFINREMTriggersLiberataCheck() throws Exception {
        setCreditAccountPaymentLiberataCheckFeature(true);
        setCheckLiberataAccountForAllServices(false);

        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFinRemJson().getBytes(), CreditAccountPaymentRequest.class);
        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);

        MvcResult result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
        verify(accountService, times(1)).retrieve(request.getAccountNumber());

        request = objectMapper.readValue(creditAccountPaymentRequestJsonWithProbateJson().getBytes(), CreditAccountPaymentRequest.class);
        result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
        verify(accountService, times(1)).retrieve(request.getAccountNumber());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void givenLiberataCheckOnAndCheckLiberataAccountForAllSericesOffThenOnlyFPLATriggersLiberataCheck() throws Exception {
        setCreditAccountPaymentLiberataCheckFeature(true);
        setCheckLiberataAccountForAllServices(false);

        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFPLAJson().getBytes(), CreditAccountPaymentRequest.class);
        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);

        MvcResult result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
        verify(accountService, times(1)).retrieve(request.getAccountNumber());

        request = objectMapper.readValue(creditAccountPaymentRequestJsonWithProbateJson().getBytes(), CreditAccountPaymentRequest.class);
        result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
        verify(accountService, times(1)).retrieve(request.getAccountNumber());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void givenLiberataCheckOffAndCheckLiberataAccountForAllSericesOnThenAllServicesTriggerLiberataCheck() throws Exception {
        setCreditAccountPaymentLiberataCheckFeature(false);
        setCheckLiberataAccountForAllServices(true);

        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFinRemJson().getBytes(), CreditAccountPaymentRequest.class);
        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);

        MvcResult result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
        verify(accountService, times(1)).retrieve(request.getAccountNumber());

        request = objectMapper.readValue(creditAccountPaymentRequestJsonWithProbateJson().getBytes(), CreditAccountPaymentRequest.class);
        result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
        verify(accountService, times(2)).retrieve(request.getAccountNumber());

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
    @WithMockUser(authorities = "payments")
    public void givenLiberataCheckOffAndCheckLiberataAccountForAllServicesOnThenAllServicesTriggerLiberataCheck_FPLA() throws Exception {
        setCreditAccountPaymentLiberataCheckFeature(false);
        setCheckLiberataAccountForAllServices(true);

        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFPLAJson().getBytes(), CreditAccountPaymentRequest.class);
        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);

        MvcResult result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
        verify(accountService, times(1)).retrieve(request.getAccountNumber());

        request = objectMapper.readValue(creditAccountPaymentRequestJsonWithProbateJson().getBytes(), CreditAccountPaymentRequest.class);
        result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
        verify(accountService, times(2)).retrieve(request.getAccountNumber());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void createCreditAccountPaymentTest_FPLService() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFPLJson().getBytes(), CreditAccountPaymentRequest.class);
        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);

        setCreditAccountPaymentLiberataCheckFeature(true);

        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void createCreditAccountPaymentTest_FPLService_InvalidSiteId() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJsonWithFPLJsonInvalidSiteId().getBytes(), CreditAccountPaymentRequest.class);
        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(1000), new BigDecimal(1000), AccountStatus.ACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);

        setCreditAccountPaymentLiberataCheckFeature(true);

        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isUnprocessableEntity());
    }

    private void setCreditAccountPaymentLiberataCheckFeature(boolean enabled) throws Exception {
        String url = "/api/ff4j/store/features/credit-account-payment-liberata-check/";
        if (enabled) {
            url += "enable";
        } else {
            url += "disable";
        }

        restActions
            .post(url,"")
            .andExpect(status().isAccepted());

    }

    private void setCheckLiberataAccountForAllServices(boolean enabled) throws Exception {
        String url = "/api/ff4j/store/features/check-liberata-account-for-all-services/";
        if (enabled) {
            url += "enable";
        } else {
            url += "disable";
        }

        restActions
            .post(url,"")
            .andExpect(status().isAccepted());
    }

    private String jsonRequestWithoutCcdCaseRefAndCaseRef() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
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
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private String creditAccountPaymentRequestJson() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"ccd_case_number\": \"CCD101\",\n" +
            "  \"case_reference\": \"12345\",\n" +
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
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private String creditAccountPaymentRequestJsonWithFinRemJson() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"ccd_case_number\": \"CCD101\",\n" +
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
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private String creditAccountPaymentRequestJsonWithProbateJson() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"ccd_case_number\": \"CCD101\",\n" +
            "  \"case_reference\": \"12345\",\n" +
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
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private String creditAccountPaymentInvalidRequestJson() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"ccd_case_number\": \"CCD101\",\n" +
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
            "  \"ccd_case_number\": \"CCD101\",\n" +
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
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private String creditAccountPaymentRequestJsonWithFPLJson() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"ccd_case_number\": \"CCD101\",\n" +
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
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private String creditAccountPaymentRequestJsonWithFPLJsonInvalidSiteId() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"ccd_case_number\": \"CCD101\",\n" +
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
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private String creditAccountPaymentRequestJsonWithFPLAJson() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"ccd_case_number\": \"CCD101\",\n" +
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
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }
}
