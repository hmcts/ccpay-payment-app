package uk.gov.hmcts.payment.api.componenttests;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.contract.*;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.service.CreditAccountPaymentService;
import uk.gov.hmcts.payment.api.util.PayStatusToPayHubStatus;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.CustomResultMatcher;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;
import uk.gov.hmcts.payment.api.v1.model.PaymentRepository;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static uk.gov.hmcts.payment.api.model.PaymentFeeLink.paymentFeeLinkWith;

@RunWith(SpringRunner.class)
@ActiveProfiles({"embedded", "local", "componenttest"})
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


    private static final String USER_ID = "user-id";

    private final static String PAYMENT_METHOD = "payment by account";

    RestActions restActions;

    @Autowired
    private ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern("dd-MM-yyyy");

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

        restActions
            .post(format("/credit-account-payments"), request)
            .andExpect(status().isCreated());
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
        Fee fee = Fee.feeWith().calculatedAmount(new BigDecimal("11.99")).version("1").code("X0001").build();

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
        Fee fee = Fee.feeWith().calculatedAmount(new BigDecimal("11.99")).version("1").code("X0001").build();

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
    public void validateCreateCreditAccountPayment_withoutCcdCaseNumberAndCaseReferenceTest() throws Exception{
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

        MvcResult result = restActions
            .post("/credit-account-payments", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
        assertEquals(paymentDto.getStatus(), "Pending");

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

    @Test
    public void searchCardPayment_withInvalidDateRanges_shouldReturn400() throws Exception {
        populateCreditAccountPaymentToDb("1");

        String startDate = LocalDate.now().toString(DATE_FORMAT);
        String endDate = startDate;

        MvcResult result = restActions
            .get("/credit-account-payments?start_date=" + startDate + "&end_date=" + endDate)
            .andExpect(status().isBadRequest())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEqualTo("Invalid input dates");
    }

    @Test
    public void searchCardPayments_withInvalidFormatDates_shouldReturn400() throws Exception {
        populateCreditAccountPaymentToDb("1");


        MvcResult result = restActions
            .get("/credit-account-payments?start_date=12/05/2018&end_date=14-05-2018")
            .andExpect(status().isBadRequest())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEqualTo("Input dates parsing exception, valid date format is dd-MM-yyyy");
    }

    @Test
    public void searchCardPayment_withEmptyDates() throws Exception{
        populateCreditAccountPaymentToDb("1");
        populateCreditAccountPaymentToDb("2");
        populateCreditAccountPaymentToDb("3");

        MvcResult result = restActions
            .get("/credit-account-payments")
            .andExpect(status().isOk())
            .andReturn();

        List<PaymentDto> payments = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<PaymentDto>>(){});
        assertThat(payments.size()).isEqualTo(0);
    }

    @Test
    public void searchCreditAccountPayments_withValidStartAndEndDates() throws Exception {
        populateCreditAccountPaymentToDb("1");
        populateCreditAccountPaymentToDb("2");
        populateCreditAccountPaymentToDb("3");

        String startDate = LocalDate.now().toString(DATE_FORMAT);
        String endDate = LocalDate.now().minus(Period.days(-1)).toString(DATE_FORMAT);

        MvcResult result = restActions
            .get("/credit-account-payments?start_date=" + startDate + "&end_date=" + endDate)
            .andExpect(status().isOk())
            .andReturn();

        List<PaymentDto> payments = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<List<PaymentDto>>(){});
        assertThat(payments.size()).isEqualTo(3);

        PaymentDto payment = payments.stream().filter(p -> p.getPaymentReference().equals("RC-1519-9028-1909-0002")).findAny().get();
        assertThat(payment.getPaymentReference()).isEqualTo("RC-1519-9028-1909-0002");
        assertThat(payment.getCcdCaseNumber()).isEqualTo("ccdCaseNumber2");
        assertThat(payment.getCaseReference()).isEqualTo("Reference2");
        assertThat(payment.getAmount()).isEqualTo(new BigDecimal("11.99"));
        assertThat(payment.getChannel()).isEqualTo("online");
        assertThat(payment.getMethod()).isEqualTo("payment by account");
        assertThat(payment.getStatus()).isEqualTo("Initiated");
        assertThat(payment.getSiteId()).isEqualTo("AA02");
        assertThat(payment.getDateCreated()).isNotNull();
        assertThat(payment.getDateUpdated()).isNotNull();
        payment.getFees().stream().forEach(f -> {
            assertThat(f.getCode()).isEqualTo("FEE0002");
            assertThat(f.getVersion()).isEqualTo("1");
            assertThat(f.getCalculatedAmount()).isEqualTo(new BigDecimal("11.99"));
        });

    }

}
