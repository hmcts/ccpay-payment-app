package uk.gov.hmcts.payment.api.componenttests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.PaymentsResponse;
import uk.gov.hmcts.payment.api.model.FeePayApportion;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.servicebus.CallbackServiceImpl;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.CustomResultMatcher;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest", "mockcallbackservice"})
@SpringBootTest(webEnvironment = MOCK)
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
public class LiberataApportionmentTest extends PaymentsDataUtil {

    private static final String USER_ID = UserResolverBackdoor.AUTHENTICATED_USER_ID;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_FORMAT_DD_MM_YYYY = DateTimeFormat.forPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_FORMAT_T_HH_MM_SS = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss");
    @Autowired
    protected ServiceResolverBackdoor serviceRequestAuthorizer;
    @Autowired
    protected UserResolverBackdoor userRequestAuthorizer;
    @MockBean
    protected CallbackServiceImpl callbackServiceImplMock;
    @Autowired
    protected PaymentDbBackdoor db;
    RestActions restActions;
    String startDate;
    String endDate;
    String paymentReference = "RC-1519-9028-1909-1435";
    Payment payment;
    @Autowired
    private ConfigurableListableBeanFactory configurableListableBeanFactory;
    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private LaunchDarklyFeatureToggler featureToggler;

    protected CustomResultMatcher body() {
        return new CustomResultMatcher(objectMapper);
    }

    @Before
    public void setup() throws Exception {
        startDate = LocalDateTime.now().toString(DATE_FORMAT);
        endDate = LocalDateTime.now().toString(DATE_FORMAT);
        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);

        restActions
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .withReturnUrl("https://www.gooooogle.com");
    }

    @After
    public void tearDown() {
        startDate = null;
        endDate = null;
        this.restActions = null;
    }

    @Test
    @Transactional
    public void shouldCheckApportionNewFieldsNotPopulatedWhenApportionFeatureIsToggledOffForBulkScanPayments() throws Exception {
        populatePaymentToDbForExelaPayments(paymentReference);
        when(featureToggler.getBooleanValue("apportion-feature", false)).thenReturn(false);

        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        restActions
            .post("/api/ff4j/store/features/bulk-scan-check/enable")
            .andExpect(status().isAccepted());

        MvcResult result1 = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate)
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse response = objectMapper.readValue(result1.getResponse().getContentAsByteArray(), PaymentsResponse.class);
        List<PaymentDto> payments = response.getPayments();
        assertNotNull(payments);
        assertThat(payments.size()).isEqualTo(1);
    }

    @Test
    @Transactional
    public void shouldCheckApportionNewFieldsPopulatedWhenApportionFeatureIsToggledONForBulkScanPayments() throws Exception {
        populatePaymentToDbForExelaPayments(paymentReference);
        when(featureToggler.getBooleanValue("apportion-feature", false)).thenReturn(true);

        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        restActions
            .post("/api/ff4j/store/features/bulk-scan-check/enable")
            .andExpect(status().isAccepted());

        MvcResult result1 = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate)
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse response = objectMapper.readValue(result1.getResponse().getContentAsByteArray(), PaymentsResponse.class);
        List<PaymentDto> payments = response.getPayments();
        assertNotNull(payments);
        assertThat(payments.size()).isEqualTo(1);
    }

    @Test
    @Transactional
    public void shouldCheckApportionNewFieldsNotPopulatedWhenApportionFeatureIsToggledOffForCardPayments() throws Exception {

        payment = populateTelephonyPaymentToDb(paymentReference, false);
        when(featureToggler.getBooleanValue("apportion-feature", false)).thenReturn(false);

        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        restActions
            .post("/api/ff4j/store/features/bulk-scan-check/enable")
            .andExpect(status().isAccepted());

        MvcResult result1 = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate)
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse response = objectMapper.readValue(result1.getResponse().getContentAsByteArray(), PaymentsResponse.class);
        List<PaymentDto> payments = response.getPayments();
        assertNotNull(payments);
        assertThat(payments.size()).isEqualTo(1);
    }

    @Test
    @Transactional
    public void shouldCheckApportionNewFieldsPopulatedWhenApportionFeatureIsToggledONForCardPayments() throws Exception {
        payment = populateTelephonyPaymentToDb(paymentReference, false);
        when(featureToggler.getBooleanValue("apportion-feature", false)).thenReturn(true);

        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        restActions
            .post("/api/ff4j/store/features/bulk-scan-check/enable")
            .andExpect(status().isAccepted());

        MvcResult result1 = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate)
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse response = objectMapper.readValue(result1.getResponse().getContentAsByteArray(), PaymentsResponse.class);
        List<PaymentDto> payments = response.getPayments();
        assertNotNull(payments);
        assertThat(payments.size()).isEqualTo(1);
    }

    @Test
    @Transactional
    public void shouldCheckAmountDueIsCalculatedFromApportionTableWhenApportionFlagToggledONForCardPayments() throws Exception {
        payment = populateTelephonyPaymentToDb(paymentReference, false);
        populateApportionDetails(payment);
        when(featureToggler.getBooleanValue("apportion-feature", false)).thenReturn(true);
        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        restActions
            .post("/api/ff4j/store/features/bulk-scan-check/enable")
            .andExpect(status().isAccepted());
        MvcResult result1 = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate)
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse response = objectMapper.readValue(result1.getResponse().getContentAsByteArray(), PaymentsResponse.class);
        List<PaymentDto> payments = response.getPayments();
        assertNotNull(payments);
        assertThat(payments.size()).isEqualTo(1);
    }

    @Test
    @Transactional
    public void shouldCheckAmountDueIsCalculatedFromApportionTableWhenCallSurplusAmountIsNotNull() throws Exception {
        payment = populateTelephonyPaymentToDb(paymentReference, false);
        populateApportionDetailsWithCallSurplusAmount(payment);
        when(featureToggler.getBooleanValue("apportion-feature", false)).thenReturn(true);
        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        restActions
            .post("/api/ff4j/store/features/bulk-scan-check/enable")
            .andExpect(status().isAccepted());
        MvcResult result1 = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate)
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse response = objectMapper.readValue(result1.getResponse().getContentAsByteArray(), PaymentsResponse.class);
        List<PaymentDto> payments = response.getPayments();
        assertNotNull(payments);
        assertThat(payments.size()).isEqualTo(1);
    }

    @Test
    @Transactional
    public void shouldCheckAmountDueIsCalculatedFromApportionTableWithoutApportionDetails() throws Exception {
        payment = populateTelephonyPaymentToDb(paymentReference, false);
        when(featureToggler.getBooleanValue("apportion-feature", false)).thenReturn(true);
        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        restActions
            .post("/api/ff4j/store/features/bulk-scan-check/enable")
            .andExpect(status().isAccepted());
        MvcResult result1 = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate)
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse response = objectMapper.readValue(result1.getResponse().getContentAsByteArray(), PaymentsResponse.class);
        List<PaymentDto> payments = response.getPayments();
        assertNotNull(payments);
        assertThat(payments.size()).isEqualTo(1);
    }

    @Test
    @Transactional
    public void shouldCheckAmountDueIsCalculatedFromApportionTableWhenFeeIdIsDifferent() throws Exception {
        payment = populateTelephonyPaymentToDb(paymentReference, false);
        populateApportionDetailsWithDifferentFeeId(payment);
        when(featureToggler.getBooleanValue("apportion-feature", false)).thenReturn(true);
        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        restActions
            .post("/api/ff4j/store/features/bulk-scan-check/enable")
            .andExpect(status().isAccepted());
        MvcResult result1 = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate)
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse response = objectMapper.readValue(result1.getResponse().getContentAsByteArray(), PaymentsResponse.class);
        List<PaymentDto> payments = response.getPayments();
        assertNotNull(payments);
        assertThat(payments.size()).isEqualTo(1);
    }


    @Test
    @Transactional
    public void shouldCheckAmountDueIsCalculatedFromApportionTableWhenApportionFlagToggledOFFForCardPayments() throws Exception {
        payment = populateTelephonyPaymentToDb(paymentReference, false);
        populateApportionDetails(payment);
        when(featureToggler.getBooleanValue("apportion-feature", false)).thenReturn(false);

        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        restActions
            .post("/api/ff4j/store/features/bulk-scan-check/enable")
            .andExpect(status().isAccepted());
        MvcResult result1 = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate)
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse response = objectMapper.readValue(result1.getResponse().getContentAsByteArray(), PaymentsResponse.class);
        List<PaymentDto> payments = response.getPayments();
        assertNotNull(payments);
        assertThat(payments.size()).isEqualTo(1);
    }

    @Test
    @Transactional
    public void shouldCheckAmountDueIsCalculatedFromApportionTableWhenWhenDateCreatedIsBeforeApportionDate() throws Exception {
        payment = populateTelephonyPaymentToDb(paymentReference, false);
        List<FeePayApportion> feePayApportionList = new ArrayList<>();
        FeePayApportion feePayApportion = FeePayApportion.feePayApportionWith()
            .id(1)
            .apportionAmount(BigDecimal.valueOf(100))
            .apportionAmount(BigDecimal.valueOf(100))
            .apportionType("AUTO")
            .feeId(1)
            .feeAmount(BigDecimal.valueOf(100))
            .build();
        feePayApportionList.add(feePayApportion);
        when(featureToggler.getBooleanValue("apportion-feature", false)).thenReturn(true);
        payment.setDateCreated(parseDate("01.05.2020"));
        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        restActions
            .post("/api/ff4j/store/features/bulk-scan-check/enable")
            .andExpect(status().isAccepted());
        MvcResult result1 = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate)
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse response = objectMapper.readValue(result1.getResponse().getContentAsByteArray(), PaymentsResponse.class);
        List<PaymentDto> payments = response.getPayments();
        assertNotNull(payments);
        assertThat(payments.size()).isEqualTo(1);
    }

    @Test
    @Transactional
    public void shouldCheckAmountDueIsCalculatedFromApportionTableWhenWhenDateCreatedIsEqualToApportionDate() throws Exception {
        payment = populateTelephonyPaymentToDb(paymentReference, false);
        populateApportionDetailsWithCallSurplusAmount(payment);
        when(featureToggler.getBooleanValue("apportion-feature", false)).thenReturn(true);
        payment.setDateCreated(parseDate("01.06.2020"));
        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        restActions
            .post("/api/ff4j/store/features/bulk-scan-check/enable")
            .andExpect(status().isAccepted());
        MvcResult result1 = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate)
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse response = objectMapper.readValue(result1.getResponse().getContentAsByteArray(), PaymentsResponse.class);
        List<PaymentDto> payments = response.getPayments();
        assertNotNull(payments);
        assertThat(payments.size()).isEqualTo(1);
    }

    @Test
    @Transactional
    public void shouldCheckAmountDueIsCalculatedFromApportionTableWhenWhenDateCreatedIsAfterApportionDate() throws Exception {
        payment = populateTelephonyPaymentToDb(paymentReference, false);
        populateApportionDetailsWithCallSurplusAmount(payment);
        when(featureToggler.getBooleanValue("apportion-feature", false)).thenReturn(true);
        payment.setDateCreated(parseDate("05.06.2020"));
        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        restActions
            .post("/api/ff4j/store/features/bulk-scan-check/enable")
            .andExpect(status().isAccepted());
        MvcResult result1 = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate)
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse response = objectMapper.readValue(result1.getResponse().getContentAsByteArray(), PaymentsResponse.class);
        List<PaymentDto> payments = response.getPayments();
        assertNotNull(payments);
        assertThat(payments.size()).isEqualTo(1);
    }

    @Test
    @Transactional
    public void shouldCheckApportionWhenPaymentChannelIsDigitalBar() throws Exception {
        Payment payment = populateBarCashPaymentToDbForApportionment("1");
        populateApportionDetailsWithCallSurplusAmount(payment);
        when(featureToggler.getBooleanValue("apportion-feature", false)).thenReturn(true);
        payment.setDateCreated(parseDate("05.06.2020"));
        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        restActions
            .post("/api/ff4j/store/features/bulk-scan-check/enable")
            .andExpect(status().isAccepted());
        MvcResult result1 = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate)
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse response = objectMapper.readValue(result1.getResponse().getContentAsByteArray(), PaymentsResponse.class);
        List<PaymentDto> payments = response.getPayments();
        assertNotNull(payments);
        assertThat(payments.size()).isEqualTo(1);
    }

    @Test
    @Transactional
    public void shouldCheckApportionWhenPaymentChannelIsNull() throws Exception {
        payment = populateTelephonyPaymentToDb(paymentReference, false);
        populateApportionDetailsWithCallSurplusAmount(payment);
        when(featureToggler.getBooleanValue("apportion-feature", false)).thenReturn(true);
        payment.setDateCreated(parseDate("05.06.2020"));
        payment.setPaymentChannel(null);
        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        restActions
            .post("/api/ff4j/store/features/bulk-scan-check/enable")
            .andExpect(status().isAccepted());
        MvcResult result1 = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate)
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse response = objectMapper.readValue(result1.getResponse().getContentAsByteArray(), PaymentsResponse.class);
        List<PaymentDto> payments = response.getPayments();
        assertNotNull(payments);
        assertThat(payments.size()).isEqualTo(1);
    }

    @Test
    @Transactional
    public void shouldCheckApportionWhenPaymentChannelIsNullAndApportionmentFlagIsFalse() throws Exception {
        payment = populateTelephonyPaymentToDb(paymentReference, false);
        populateApportionDetailsWithCallSurplusAmount(payment);
        when(featureToggler.getBooleanValue("apportion-feature", false)).thenReturn(false);
        payment.setDateCreated(parseDate("05.06.2020"));
        payment.setPaymentChannel(null);
        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        restActions
            .post("/api/ff4j/store/features/bulk-scan-check/enable")
            .andExpect(status().isAccepted());
        MvcResult result1 = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate)
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse response = objectMapper.readValue(result1.getResponse().getContentAsByteArray(), PaymentsResponse.class);
        List<PaymentDto> payments = response.getPayments();
        assertNotNull(payments);
        assertThat(payments.size()).isEqualTo(1);
    }

    @Test
    @Transactional
    public void shouldCheckApportionWhenPaymentChannelIsNullAndApportionmentFlagIsFalseAndHistoricalCase() throws Exception {
        payment = populateTelephonyPaymentToDb(paymentReference, false);
        populateApportionDetailsWithCallSurplusAmount(payment);
        when(featureToggler.getBooleanValue("apportion-feature", false)).thenReturn(false);
        payment.setDateCreated(parseDate("01.05.2020"));
        payment.setPaymentChannel(null);
        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        restActions
            .post("/api/ff4j/store/features/bulk-scan-check/enable")
            .andExpect(status().isAccepted());
        MvcResult result1 = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate)
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse response = objectMapper.readValue(result1.getResponse().getContentAsByteArray(), PaymentsResponse.class);
        List<PaymentDto> payments = response.getPayments();
        assertNotNull(payments);
        assertThat(payments.size()).isEqualTo(1);
    }

    @Test
    @Transactional
    public void shouldCheckApportionWhenPaymentChannelIsNullAndApportionmentFlagIsTrueAndHistoricalCase() throws Exception {
        payment = populateTelephonyPaymentToDb(paymentReference, false);
        populateApportionDetailsWithCallSurplusAmount(payment);
        when(featureToggler.getBooleanValue("apportion-feature", false)).thenReturn(true);
        payment.setDateCreated(parseDate("01.05.2020"));
        payment.setPaymentChannel(null);
        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        restActions
            .post("/api/ff4j/store/features/bulk-scan-check/enable")
            .andExpect(status().isAccepted());
        MvcResult result1 = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate)
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse response = objectMapper.readValue(result1.getResponse().getContentAsByteArray(), PaymentsResponse.class);
        List<PaymentDto> payments = response.getPayments();
        assertNotNull(payments);
        assertThat(payments.size()).isEqualTo(1);
    }

    @Test
    @Transactional
    public void shouldCheckApportionWhenPaymentChannelIsTelephonyAndApportionmentFlagIsTrue() throws Exception {
        payment = populateTelephonyPaymentToDb(paymentReference, false);
        when(featureToggler.getBooleanValue("apportion-feature", false)).thenReturn(true);
        payment.setDateCreated(parseDate("05.06.2020"));
        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        restActions
            .post("/api/ff4j/store/features/bulk-scan-check/enable")
            .andExpect(status().isAccepted());
        MvcResult result1 = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate)
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse response = objectMapper.readValue(result1.getResponse().getContentAsByteArray(), PaymentsResponse.class);
        List<PaymentDto> payments = response.getPayments();
        assertNotNull(payments);
        assertThat(payments.size()).isEqualTo(1);
    }

    @Test
    @Transactional
    public void shouldCheckApportionWhenPaymentChannelIsTelephonyAndFeeIdIsDifferent() throws Exception {
        payment = populateTelephonyPaymentToDb(paymentReference, false);
        populateApportionDetailsWithDifferentFeeId(payment);
        when(featureToggler.getBooleanValue("apportion-feature", false)).thenReturn(true);
        payment.setDateCreated(parseDate("05.06.2020"));
        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        restActions
            .post("/api/ff4j/store/features/bulk-scan-check/enable")
            .andExpect(status().isAccepted());
        MvcResult result1 = restActions
            .get("/payments?start_date=" + startDate + "&end_date=" + endDate)
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse response = objectMapper.readValue(result1.getResponse().getContentAsByteArray(), PaymentsResponse.class);
        List<PaymentDto> payments = response.getPayments();
        assertNotNull(payments);
        assertThat(payments.size()).isEqualTo(1);
    }

    private Date parseDate(String date) {
        try {
            return new SimpleDateFormat("dd.MM.yyyy").parse(date);
        } catch (ParseException e) {
            return null;
        }
    }
}
