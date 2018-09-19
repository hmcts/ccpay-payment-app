package uk.gov.hmcts.payment.api.componenttests;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.ff4j.services.domain.FeatureApiBean;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.PaymentsResponse;
import uk.gov.hmcts.payment.api.contract.UpdatePaymentRequest;
import uk.gov.hmcts.payment.api.contract.exception.ValidationErrorDTO;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.util.DateUtil;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.CustomResultMatcher;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static uk.gov.hmcts.payment.api.model.PaymentFeeLink.paymentFeeLinkWith;

@RunWith(SpringRunner.class)
@ActiveProfiles({"embedded", "local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@Transactional
public class PaymentControllerTest extends PaymentsDataUtil {

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
    protected DateUtil dateUtil;

    private static final String USER_ID = UserResolverBackdoor.AUTHENTICATED_USER_ID;

    private final static String PAYMENT_METHOD = "payment by account";

    RestActions restActions;

    @Autowired
    private ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd");

    private static final DateTimeFormatter DATE_FORMAT_DD_MM_YYYY = DateTimeFormat.forPattern("dd-MM-yyyy");

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

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
    public void updateCaseReference_forGivenPaymentReferenceTest() throws Exception {
        //Create a payment in db
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("11.99"))
            .caseReference("caseReference")
            .ccdCaseNumber("ccdCaseNumber")
            .description("Description1")
            .serviceType("Probate")
            .currency("GBP")
            .siteId("AA01")
            .userId(USER_ID)
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("payment by account").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .reference("RC-1519-9028-1909-3890")
            .build();
        PaymentFee fee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("11.99")).version("1").code("X0001").build();

        PaymentFeeLink paymentFeeLink = db.create(paymentFeeLinkWith().paymentReference("2018-15186168000").payments(Arrays.asList(payment)).fees(Arrays.asList(fee)));
        payment.setPaymentLink(paymentFeeLink);
        Payment savedPayment = paymentFeeLink.getPayments().get(0);

        MvcResult result1 = restActions.
            get(format("/credit-account-payments/RC-1519-9028-1909-3890"))
            .andExpect(status().isOk())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result1.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertEquals(paymentDto.getCaseReference(), "caseReference");
        assertEquals(paymentDto.getCcdCaseNumber(), "ccdCaseNumber");

        /* -- update payment -- */
        UpdatePaymentRequest updatePaymentRequest = objectMapper.readValue(updatePaymentRequestJson().getBytes(), UpdatePaymentRequest.class);
        MvcResult result2 = restActions.
            patch(format("/payments/" + paymentDto.getReference()), updatePaymentRequest)
            .andExpect(status().isNoContent())
            .andReturn();
    }

    @Test
    public void upateCaseReferenceValidation_forEmptyValues() throws Exception{
        UpdatePaymentRequest updatePaymentRequest = objectMapper.readValue(updatePaymentInvalidRequestJson().getBytes(), UpdatePaymentRequest.class);
        MvcResult result = restActions.
            patch(format("/payments/RC-1519-9028-1909-3111"), updatePaymentRequest)
            .andExpect(status().isUnprocessableEntity())
            .andReturn();
    }


    private String updatePaymentRequestJson() {
        return "{\n" +
            "  \"case_reference\": \"newCaseReference\",\n" +
            "  \"ccd_case_number\": \"newCcdCaseNumber\"\n" +
            "}";
    }


    private String updatePaymentInvalidRequestJson() {
        return "{\n" +
            "  \"case_reference\": \"\"\n" +
            "}";
    }

    @Test
    @Transactional
    public void searchAllPayments_withValidBetweenDates_shouldReturn200() throws Exception {
        populateCardPaymentToDb("1");
        populateCreditAccountPaymentToDb("2");

        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());


        String startDate = LocalDate.now().toString(DATE_FORMAT);
        String endDate = LocalDate.now().toString(DATE_FORMAT);

        MvcResult result = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate)
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse paymentsResponse = objectMapper.readValue(result.getResponse().getContentAsString(), PaymentsResponse.class);

        assertThat(paymentsResponse.getPayments().size()).isEqualTo(2);
    }

    @Test
    @Transactional
    public void searchAllPayments_withCcdCaseNumber_shouldReturnRequiredFieldsForVisualComponent() throws Exception {
        populateCardPaymentToDb("1");
        populateCreditAccountPaymentToDb("1");

        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        MvcResult result = restActions
            .get("/payments?ccd_case_number=ccdCaseNumber1")
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse payments = objectMapper.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<PaymentsResponse>(){});

        assertThat(payments.getPayments().size()).isEqualTo(2);

        PaymentDto payment = payments.getPayments().get(0);

        assertThat(payment.getCcdCaseNumber()).isEqualTo("ccdCaseNumber1");

        assertThat(payment.getReference()).isNotBlank();
        assertThat(payment.getAmount()).isPositive();
        assertThat(payment.getDateCreated()).isNotNull();
        assertThat(payment.getCustomerReference()).isNotBlank();
    }

    @Test
    @Transactional
    public void searchCardPayments_withValidBetweenDates_shouldReturnOnlyCardPayments() throws Exception {

        populateCardPaymentToDb("2");
        populateCreditAccountPaymentToDb("1");

        String startDate = LocalDate.now().toString(DATE_FORMAT);
        String endDate = LocalDate.now().toString(DATE_FORMAT);

        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        MvcResult result = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate + "&payment_method=CARD")
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse paymentsResponse = objectMapper.readValue(result.getResponse().getContentAsString(), PaymentsResponse.class);
        List<PaymentDto> payments = paymentsResponse.getPayments();
        assertThat(payments.size()).isEqualTo(1);
        payments.stream().forEach(p -> {
            assertThat(p.getPaymentReference()).isEqualTo("RC-1519-9028-2432-0002");
            assertThat(p.getCcdCaseNumber()).isEqualTo("ccdCaseNumber2");
            assertThat(p.getCaseReference()).isEqualTo("Reference2");
            assertThat(p.getAmount()).isEqualTo(new BigDecimal("99.99"));
            assertThat(p.getChannel()).isEqualTo("online");
            assertThat(p.getMethod()).isEqualTo("card");
            assertThat(p.getStatus()).isEqualTo("Initiated");
            assertThat(p.getSiteId()).isEqualTo("AA02");
            assertThat(p.getDateCreated()).isNotNull();
            assertThat(p.getDateUpdated()).isNotNull();
            p.getFees().stream().forEach(f -> {
                assertThat(f.getCode()).isEqualTo("FEE0002");
                assertThat(f.getVersion()).isEqualTo("1");
                assertThat(f.getCalculatedAmount()).isEqualTo(new BigDecimal("99.99"));
            });
        });
    }

    @Test
    @Transactional
    public void searchCreditPayments_withValidBetweenDates_shouldReturnOnlyPbaPayments() throws Exception {

        populateCardPaymentToDb("1");
        populateCreditAccountPaymentToDb("2");

        String startDate = LocalDate.now().toString(DATE_FORMAT);
        String endDate = LocalDate.now().toString(DATE_FORMAT);

        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        MvcResult result = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate + "&payment_method=PBA")
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse paymentsResponse = objectMapper.readValue(result.getResponse().getContentAsString(), PaymentsResponse.class);
        List<PaymentDto> payments = paymentsResponse.getPayments();
        assertThat(payments.size()).isEqualTo(1);
        payments.stream().forEach(p -> {
            assertThat(p.getPaymentReference()).isEqualTo("RC-1519-9028-1909-0002");
            assertThat(p.getCcdCaseNumber()).isEqualTo("ccdCaseNumber2");
            assertThat(p.getCaseReference()).isEqualTo("Reference2");
            assertThat(p.getAmount()).isEqualTo(new BigDecimal("11.99"));
            assertThat(p.getChannel()).isEqualTo("online");
            assertThat(p.getMethod()).isEqualTo("payment by account");
            assertThat(p.getStatus()).isEqualTo("Initiated");
            assertThat(p.getSiteId()).isEqualTo("AA02");
            assertThat(p.getDateCreated()).isNotNull();
            assertThat(p.getDateUpdated()).isNotNull();
            p.getFees().stream().forEach(f -> {
                assertThat(f.getCode()).isEqualTo("FEE0002");
                assertThat(f.getVersion()).isEqualTo("1");
                assertThat(f.getCalculatedAmount()).isEqualTo(new BigDecimal("11.99"));
            });
        });
    }

    @Test
    @Transactional
    public void searchAllPayments_withInvalidMethodType_shouldReturn400() throws Exception {
        populateCardPaymentToDb("1");

        String startDate = LocalDate.now().toString(DATE_FORMAT);
        String endDate = startDate;

        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        MvcResult result = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate + "&payment_method=UNKNOWN")
            .andExpect(status().isBadRequest())
            .andReturn();

        ValidationErrorDTO errorDTO = objectMapper.readValue(result.getResponse().getContentAsString(), ValidationErrorDTO.class);
        assertThat(errorDTO.hasErrors()).isTrue();
        assertThat(errorDTO.getFieldErrors().size()).isEqualTo(1);
        assertThat(errorDTO.getFieldErrors().get(0).getMessage()).isEqualTo("Invalid payment method requested");
    }

    @Test
    @Transactional
    public void searchAllPayments_withInvalidServiceType_shouldReturn400() throws Exception {
        populateCardPaymentToDb("1");

        String startDate = LocalDate.now().toString(DATE_FORMAT);
        String endDate = startDate;

        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        MvcResult result = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate + "&service_name=UNKNOWN")
            .andExpect(status().isBadRequest())
            .andReturn();

        ValidationErrorDTO errorDTO = objectMapper.readValue(result.getResponse().getContentAsString(), ValidationErrorDTO.class);
        assertThat(errorDTO.hasErrors()).isTrue();
        assertThat(errorDTO.getFieldErrors().size()).isEqualTo(1);
        assertThat(errorDTO.getFieldErrors().get(0).getMessage()).isEqualTo("Invalid service name requested");
    }

    @Test
    @Transactional
    public void searchAllPayment_withFutureDate_shouldReturn400() throws Exception {
        populateCardPaymentToDb("1");

        String startDate = LocalDate.now().toString(DATE_FORMAT);
        String endDate = LocalDate.now().plusDays(1).toString(DATE_FORMAT);

        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        MvcResult result = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate + "&payment_method=CARD")
            .andExpect(status().isBadRequest())
            .andReturn();

        ValidationErrorDTO errorDTO = objectMapper.readValue(result.getResponse().getContentAsString(), ValidationErrorDTO.class);
        assertThat(errorDTO.hasErrors()).isTrue();
        assertThat(errorDTO.getFieldErrors().size()).isEqualTo(1);
        assertThat(errorDTO.getFieldErrors().get(0).getField()).isEqualTo("end_date");
        assertThat(errorDTO.getFieldErrors().get(0).getMessage()).isEqualTo("Date cannot be in the future");
    }

    @Test
    @Transactional
    public void searchAllPayments_withInvalidFormatDates_shouldReturn400() throws Exception {
        populateCardPaymentToDb("1");

        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        MvcResult result = restActions
            .get("/payments?start_date=12/05/2018&end_date=14-05-2018&payment_method=CARD")
            .andExpect(status().isBadRequest())
            .andReturn();

        ValidationErrorDTO errorDTO = objectMapper.readValue(result.getResponse().getContentAsString(), ValidationErrorDTO.class);
        assertThat(errorDTO.hasErrors()).isTrue();
        assertThat(errorDTO.getFieldErrors().size()).isEqualTo(2);
        assertThat(errorDTO.getFieldErrors().get(0).getField()).isEqualTo("dates");
        assertThat(errorDTO.getFieldErrors().get(0).getMessage()).isEqualTo("Invalid date format, required date format is ISO.");
        assertThat(errorDTO.getFieldErrors().get(1).getField()).isEqualTo("start_date");
        assertThat(errorDTO.getFieldErrors().get(1).getMessage()).isEqualTo("Invalid date format received, required data format is ISO");
    }

    @Test
    public void testFindFeatureFlag_withCorrectUID() throws Exception {
        MvcResult result = restActions
            .get("/api/ff4j/store/features/payment-search")
            .andExpect(status().isOk())
            .andReturn();

        FeatureApiBean feature = objectMapper.readValue(result.getResponse().getContentAsByteArray(), FeatureApiBean.class);
        assertThat(feature.getUid()).isEqualTo("payment-search");
        assertThat(feature.isEnable()).isEqualTo(true);
        assertThat(feature.getDescription()).isEqualTo("Payments search API");
    }

    @Test
    public void testFindFeatureFlag_withIncorrectUID_shouldReturn404() throws Exception {
        restActions
            .get("/api/ff4j/store/features/my-feature")
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void testEnableFeatureFlag_withCorrectUID() throws Exception {
        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        MvcResult result = restActions
            .get("/api/ff4j/store/features/payment-search")
            .andExpect(status().isOk())
            .andReturn();

        FeatureApiBean feature = objectMapper.readValue(result.getResponse().getContentAsByteArray(), FeatureApiBean.class);
        assertThat(feature.getUid()).isEqualTo("payment-search");
        assertThat(feature.isEnable()).isEqualTo(true);
        assertThat(feature.getDescription()).isEqualTo("Payments search API");
    }

    @Test
    public void testRetrievePayments_withFeatureFlagDisabled_shouldReturnValidMessage() throws Exception {
        restActions
            .post("/api/ff4j/store/features/payment-search/disable")
            .andExpect(status().isAccepted());

        MvcResult result = restActions
            .get("/payments?payment_method=ALL")
            .andExpect(status().isBadRequest())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEqualTo("Payment search feature is not available for usage.");
    }


    @Test
    @Transactional
    public void searchCardPayments_withValidEndDateAndNoStartDate_shouldReturnOk() throws Exception {

        populateCardPaymentToDb("2");
        populateCreditAccountPaymentToDb("1");

        String startDate = LocalDate.now().toString(DATE_FORMAT);
        String endDate = LocalDateTime.now().toString(DATE_TIME_FORMAT);

        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        MvcResult result = restActions
            .get("/payments?payment_method=CARD&start_date=" + startDate + "&end_date=" + endDate)
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse paymentsResponse = objectMapper.readValue(result.getResponse().getContentAsString(), PaymentsResponse.class);
        List<PaymentDto> payments = paymentsResponse.getPayments();
        assertThat(payments.size()).isEqualTo(1);
        payments.stream().forEach(p -> {
            System.out.println("date updated: " + p.getDateUpdated());
            assertThat(p.getPaymentReference()).isEqualTo("RC-1519-9028-2432-0002");
            assertThat(p.getCcdCaseNumber()).isEqualTo("ccdCaseNumber2");
            assertThat(p.getCaseReference()).isEqualTo("Reference2");
            assertThat(p.getAmount()).isEqualTo(new BigDecimal("99.99"));
            assertThat(p.getChannel()).isEqualTo("online");
            assertThat(p.getMethod()).isEqualTo("card");
            assertThat(p.getStatus()).isEqualTo("Initiated");
            assertThat(p.getSiteId()).isEqualTo("AA02");
            assertThat(p.getDateCreated()).isNotNull();
            assertThat(p.getDateUpdated()).isNotNull();
            p.getFees().stream().forEach(f -> {
                assertThat(f.getCode()).isEqualTo("FEE0002");
                assertThat(f.getVersion()).isEqualTo("1");
                assertThat(f.getCalculatedAmount()).isEqualTo(new BigDecimal("99.99"));
            });
        });
    }

    @Test
    @Transactional
    public void searchAllPaymentsWithoutEndDateShouldFail() throws Exception {
        String startDate = LocalDate.now().toString("dd/MM/yyyy");

        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        MvcResult result = restActions
            .get("/payments?start_date=" + startDate)
            .andExpect(status().isBadRequest())
            .andReturn();

        ValidationErrorDTO errorDTO = objectMapper.readValue(result.getResponse().getContentAsString(), ValidationErrorDTO.class);
        assertThat(errorDTO.hasErrors()).isTrue();
        assertThat(errorDTO.getFieldErrors().get(0).getMessage()).isEqualTo("Both start and end dates are required.");
    }

    @Test
    @Transactional
    public void searchAllPaymentsWithInvalidDateFormatshouldFail() throws Exception {
        populateCardPaymentToDb("1");

        String startDate = LocalDate.now().toString("dd/MM/yyyy");
        String endDate = startDate;

        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        MvcResult result = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate)
            .andExpect(status().isBadRequest())
            .andReturn();

        ValidationErrorDTO errorDTO = objectMapper.readValue(result.getResponse().getContentAsString(), ValidationErrorDTO.class);
        assertThat(errorDTO.hasErrors()).isTrue();
        assertThat(errorDTO.getFieldErrors().get(0).getMessage()).isEqualTo("Invalid date format, required date format is ISO.");
    }

    @Test
    @Transactional
    public void searchAllPaymentsWithValidDateFormatShouldPass() throws Exception {
        populateBarCashPaymentToDb("1");
        populateBarCardPaymentToDb("2");

        String startDate = LocalDateTime.now().toString(DATE_FORMAT);
        String endDate = LocalDateTime.now().toString(DATE_FORMAT);

        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        MvcResult result1 = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate)
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse response = objectMapper.readValue(result1.getResponse().getContentAsByteArray(), PaymentsResponse.class);
        List<PaymentDto> payments = response.getPayments();
        assertNotNull(payments);
        assertThat(payments.size()).isEqualTo(2);
    }

    @Test
    @Transactional
    public void searchAllPaymentsWithValidDateTimeFormatShouldPass() throws Exception {
        populateBarCashPaymentToDb("3");
        populateBarChequePaymentToDb("4");

        String startDate = LocalDate.now().toString(DATE_FORMAT);
        String endDate = LocalDate.now().toString(DATE_FORMAT);

        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        MvcResult result1 = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate)
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse response = objectMapper.readValue(result1.getResponse().getContentAsByteArray(), PaymentsResponse.class);
        List<PaymentDto> payments = response.getPayments();
        assertNotNull(payments);
        assertThat(payments.size()).isEqualTo(2);
    }

    @Test
    @Transactional
    public void searchAllPaymentWithDateDDMMYYYYFormatShouldPass() throws Exception {
        populateBarCashPaymentToDb("5");
        populateBarChequePaymentToDb("6");

        String startDate = LocalDateTime.now().toString(DATE_FORMAT_DD_MM_YYYY);
        String endDate = LocalDateTime.now().minusSeconds(1).toString(DATE_FORMAT_DD_MM_YYYY);


        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        MvcResult result1 = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate)
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse response = objectMapper.readValue(result1.getResponse().getContentAsByteArray(), PaymentsResponse.class);
        List<PaymentDto> payments = response.getPayments();
        assertNotNull(payments);
        assertThat(payments.size()).isEqualTo(2);

    }

}
