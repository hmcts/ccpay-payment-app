package uk.gov.hmcts.payment.api.componenttests;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.joda.time.LocalDate;
import org.joda.time.Period;
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
import uk.gov.hmcts.payment.api.model.*;
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
        Fee fee = Fee.feeWith().calculatedAmount(new BigDecimal("11.99")).version("1").code("X0001").build();

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
    public void searchAllPayments_withValidBetweenDates_shouldReturn200() throws Exception {
        populateCardPaymentToDb("1");
        populateCreditAccountPaymentToDb("2");

        String startDate = LocalDate.now().toString(DATE_FORMAT);
        String endDate = LocalDate.now().minus(Period.days(-1)).toString(DATE_FORMAT);

        MvcResult result = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate + "&payment_method=ALL")
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse paymentsResponse = objectMapper.readValue(result.getResponse().getContentAsString(), PaymentsResponse.class);

        assertThat(paymentsResponse.getPayments().size()).isEqualTo(2);
    }

    @Test
    public void searchCardPayments_withValidBetweenDates_shouldReturnOnlyCardPayments() throws Exception {
        populateCardPaymentToDb("2");
        populateCreditAccountPaymentToDb("1");

        String startDate = LocalDate.now().toString(DATE_FORMAT);
        String endDate = LocalDate.now().minus(Period.days(-1)).toString(DATE_FORMAT);

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
    public void searchCreditPayments_withValidBetweenDates_shouldReturnOnlyPbaPayments() throws Exception {
        populateCardPaymentToDb("1");
        populateCreditAccountPaymentToDb("2");

        String startDate = LocalDate.now().toString(DATE_FORMAT);
        String endDate = LocalDate.now().minus(Period.days(-1)).toString(DATE_FORMAT);

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
    public void searchAllPayment_withInvalidDateRanges_shouldReturn400() throws Exception {
        populateCardPaymentToDb("1");

        String startDate = LocalDate.now().toString(DATE_FORMAT);
        String endDate = startDate;

        MvcResult result = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate + "&payment_method=ALL")
            .andExpect(status().isBadRequest())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEqualTo("Invalid input dates");
    }

    @Test
    public void searchAllPayments_withInvalidFormatDates_shouldReturn400() throws Exception {
        populateCardPaymentToDb("1");


        MvcResult result = restActions
            .get("/payments?start_date=12/05/2018&end_date=14-05-2018&payment_method=ALL")
            .andExpect(status().isBadRequest())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEqualTo("Input dates parsing exception, valid date format is dd-MM-yyyy");
    }

}
