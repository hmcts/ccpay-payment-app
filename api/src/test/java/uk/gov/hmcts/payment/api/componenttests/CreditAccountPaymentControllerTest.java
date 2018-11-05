package uk.gov.hmcts.payment.api.componenttests;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.contract.CreditAccountPaymentRequest;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.exception.AccountServiceUnavailableException;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentChannel;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentMethod;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.model.StatusHistory;
import uk.gov.hmcts.payment.api.service.AccountService;
import uk.gov.hmcts.payment.api.util.AccountStatus;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.CustomResultMatcher;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static uk.gov.hmcts.payment.api.model.PaymentFeeLink.paymentFeeLinkWith;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@Transactional
public class CreditAccountPaymentControllerTest extends PaymentsDataUtil {

    private final static String PAYMENT_REFERENCE_REFEX = "^[RC-]{3}(\\w{4}-){3}(\\w{4}){1}";

    @Autowired
    private ConfigurableListableBeanFactory configurableListableBeanFactory;

    @Autowired
    private WebApplicationContext webApplicationContext;

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

    private static final String USER_ID = UserResolverBackdoor.AUTHENTICATED_USER_ID;

    RestActions restActions;

    @Autowired
    private ObjectMapper objectMapper;

    protected CustomResultMatcher body() {
        return new CustomResultMatcher(objectMapper);
    }

    @SneakyThrows
    String contentsOf(String fileName) {
        String content = new String(Files.readAllBytes(Paths.get(ResourceUtils.getURL("classpath:" + fileName).toURI())));
        return resolvePlaceholders(content);
    }

    String resolvePlaceholders(String content) {
        return configurableListableBeanFactory.resolveEmbeddedValue(content);
    }


    @Before
    public void setup() {
        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);

        restActions
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .withReturnUrl("https://www.gooooogle.com");
    }


    @Test
    public void createCreditAccountPaymentTest() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJson().getBytes(), CreditAccountPaymentRequest.class);
        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(100), new BigDecimal(100), AccountStatus.ACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);
        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isCreated());

        Mockito.reset(accountService);
    }

    @Test
    public void retrieveCreditAccountPaymentByPaymentReference() throws Exception {
        //Create a payment in db
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
        //Create a payment in db
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
    public void validateCreateCreditAccountPayment_withoutCcdCaseNumberAndCaseReferenceTest() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(jsonRequestWithoutCcdCaseRefAndCaseRef().getBytes(), CreditAccountPaymentRequest.class);

        MvcResult res = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        assertEquals(res.getResponse().getContentAsString(), "eitherOneRequired: Either ccdCaseNumber or caseReference is required.");
    }

    @Test
    public void createCreditAccountPayment_withEitherCcdCaseNumberOrCaseReferenceTest() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(jsonRequestWithCaseReference().getBytes(), CreditAccountPaymentRequest.class);
        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(100), new BigDecimal(100), AccountStatus.ACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);

        MvcResult result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
        assertEquals(paymentDto.getStatus(), "Success");

        Mockito.reset(accountService);
    }

    @Test
    public void createCreditAccountAndLiberataRespondsAccountIsActiveShouldReturnPaymentSuccess() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJson().getBytes(),
            CreditAccountPaymentRequest.class);
        AccountDto accountActiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(100), new BigDecimal(100), AccountStatus.ACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountActiveDto);

        MvcResult result = restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isCreated()).andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        assertEquals("Success", paymentDto.getStatus());

        Mockito.reset(accountService);
    }

    @Test
    public void createCreditAccountAndLiberataRespondsAccountIsInactiveShouldReturnPaymentFailed() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJson().getBytes(), CreditAccountPaymentRequest.class);
        AccountDto accountInactiveDto = new AccountDto(request.getAccountNumber(), "accountName",
            new BigDecimal(100), new BigDecimal(100), AccountStatus.INACTIVE, new Date());
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenReturn(accountInactiveDto);

        MvcResult result = restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isCreated()).andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        assertEquals("Failed", paymentDto.getStatus());

        Mockito.reset(accountService);
    }

    @Test
    public void createCreditAccountAndLiberataRespondsCannotFindAccountShouldReturn404() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJson().getBytes(), CreditAccountPaymentRequest.class);
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenThrow(HttpClientErrorException.class);

        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isNotFound());

        Mockito.reset(accountService);
    }

    @Test
    public void createCreditAccountAndLiberataIsNotResponsiveShouldReturn504() throws Exception {
        CreditAccountPaymentRequest request = objectMapper.readValue(creditAccountPaymentRequestJson().getBytes(), CreditAccountPaymentRequest.class);
        Mockito.when(accountService.retrieve(request.getAccountNumber())).thenThrow(AccountServiceUnavailableException.class);

        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isGatewayTimeout());

        Mockito.reset(accountService);
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
}
