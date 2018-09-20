package uk.gov.hmcts.payment.api.componenttests;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.validator.routines.checkdigit.CheckDigit;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;
import org.joda.time.DateTime;
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
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.PaymentsResponse;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.dto.PaymentRecordRequest;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.CustomResultMatcher;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@Transactional
public class PaymentRecordControllerTest {

    private final static String PAYMENT_REFERENCE_REFEX = "^[RC-]{3}(\\w{4}-){3}(\\w{4}){1}";

    @Autowired
    private ConfigurableListableBeanFactory configurableListableBeanFactory;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    private UserResolverBackdoor userRequestAuthorizer;


    private static final String USER_ID = UserResolverBackdoor.AUTHENTICATED_USER_ID;

    private RestActions restActions;

    @Autowired
    private ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern("dd-MM-yyyy");

    protected CustomResultMatcher body() {
        return new CustomResultMatcher(objectMapper);
    }

    private CheckDigit cd;

    @SneakyThrows
    private String contentsOf(String fileName) {
        String content = new String(Files.readAllBytes(Paths.get(ResourceUtils.getURL("classpath:" + fileName).toURI())));
        return resolvePlaceholders(content);
    }

    private String resolvePlaceholders(String content) {
        return configurableListableBeanFactory.resolveEmbeddedValue(content);
    }


    @Before
    public void setup() {
        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);
        cd = new LuhnCheckDigit();

        restActions
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .withReturnUrl("https://www.gooooogle.com");

    }

    @Test
    public void testRecordCashPaymentWithValidData() throws Exception {
        PaymentRecordRequest request = getCashPaymentRequest();

        MvcResult result = restActions
            .post("/payment-records", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto response = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertThat(response).isNotNull();
        assertThat(response.getPaymentGroupReference()).isNotNull();
        assertThat(response.getReference().matches(PAYMENT_REFERENCE_REFEX)).isEqualTo(true);
        assertThat(response.getStatus()).isEqualTo("Initiated");

        String reference = response.getReference().substring(3, response.getReference().length());
        assertThat(cd.isValid(reference.replace("-", ""))).isEqualTo(true);
    }

    @Test
    public void testRecordPaymentWithoutPaymentMethod() throws Exception {
        PaymentRecordRequest request = getRequestWithNoCcdCaseNumberAndCaseReference();
        request.setReference("ref_123");

        MvcResult result = restActions
            .post("/payment-records", request)
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEqualTo("paymentMethod: must not be null");
    }

    @Test
    public void testRecordChequePaymentWithoutReference() throws Exception {
        PaymentRecordRequest request = getRequestWithNoCcdCaseNumberAndCaseReference();
        request.setPaymentMethod(PaymentMethodType.CHEQUE);

        MvcResult result = restActions
            .post("/payment-records", request)
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEqualTo("reference: must not be empty");
    }

    @Test
    public void testRecordCashPaymentWithInvalidRequest() throws Exception {
        PaymentRecordRequest request = getRequestWithNoCcdCaseNumberAndCaseReference();

        restActions
            .post("/payment-records", request)
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @Transactional
    public void testGetBarPaymentsForBetweenDates() throws Exception {
        PaymentRecordRequest cashPaymentRequest = getCashPaymentRequest();
        restActions
            .post("/payment-records", cashPaymentRequest)
            .andExpect(status().isCreated());

        PaymentRecordRequest chequePaymentRequest = getChequePaymentRequest();
        restActions
            .post("/payment-records", chequePaymentRequest)
            .andExpect(status().isCreated());


        MvcResult result = restActions
            .get("/payments?payment_method=cheque&service_name=DIGITAL_BAR")
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse paymentsResponse = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentsResponse.class);
        assertThat(paymentsResponse).isNotNull();
        List<PaymentDto> paymentDtos = paymentsResponse.getPayments();

        Optional<PaymentDto> optPaymentDto = paymentDtos.stream().filter(p -> p.getMethod().equals("cash")).findAny();
        if (optPaymentDto.isPresent()) {
            PaymentDto paymentDto = optPaymentDto.get();
            assertThat(paymentDto.getChannel()).isEqualTo("digital bar");
            assertThat(paymentDto.getGiroSlipNo()).isEqualTo("12345");
            assertThat(paymentDto.getMethod()).isEqualTo("cash");
            paymentDto.getFees().stream().forEach(f -> {
                assertThat(f.getCode()).isEqualTo("FEE0123");
                assertThat(f.getReference()).isEqualTo("ref_123");
            });
            assertThat(paymentDto.getReportedDateOffline()).isNotNull();
        }

        Optional<PaymentDto> optChequePayment = paymentDtos.stream().filter(p -> p.getMethod().equals("cheque")).findAny();
        if (optChequePayment.isPresent()) {
            PaymentDto chequePayment = optChequePayment.get();
            assertThat(chequePayment.getChannel()).isEqualTo("digital bar");
            assertThat(chequePayment.getExternalProvider()).isEqualTo("middle office provider");
            assertThat(chequePayment.getExternalReference()).isEqualTo("1000012");
            assertThat(chequePayment.getMethod()).isEqualTo("cheque");
            assertThat(chequePayment.getGiroSlipNo()).isEqualTo("434567");
            chequePayment.getFees().stream().forEach(f -> {
                assertThat(f.getCode()).isEqualTo("FEE0111");
                assertThat(f.getReference()).isEqualTo("ref_122");
            });
            assertThat(chequePayment.getReportedDateOffline()).isNotNull();
        }
    }

    public void testRecordPostOrderPayment() throws Exception {
        PaymentRecordRequest request = getPostalOrderPaymentRequest();

        MvcResult result = restActions
            .post("/payment-records", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto response = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertThat(response).isNotNull();
        assertThat(response.getPaymentGroupReference()).isNotNull();
        assertThat(response.getReference().matches(PAYMENT_REFERENCE_REFEX)).isEqualTo(true);
        assertThat(response.getStatus()).isEqualTo("Initiated");
    }

    @Test
    public void testRecordBarclaycardPayment() throws Exception {
        PaymentRecordRequest request = getBarclayCardPaymentRequest();

        MvcResult result = restActions
            .post("/payment-records", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto response = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertThat(response.getPaymentGroupReference()).isNotNull();
        assertThat(response.getReference().matches(PAYMENT_REFERENCE_REFEX)).isEqualTo(true);
        assertThat(response.getStatus()).isEqualTo("Initiated");
    }

    @Test
    public void testNoProviderPayment() throws Exception {
        PaymentRecordRequest request = getNoProviderPaymentRequest();

        MvcResult result = restActions
            .post("/payment-records", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto response = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertThat(response.getPaymentGroupReference()).isNotNull();
        assertThat(response.getReference().matches(PAYMENT_REFERENCE_REFEX)).isEqualTo(true);
        assertThat(response.getStatus()).isEqualTo("Initiated");
    }

    @Test
    public void testBarclaycardPaymentWithoutReportedDateOfflineShouldFail() throws Exception {
        PaymentRecordRequest request = getBarclayCardPaymentRequestWithoutReportedDateOffline();

        MvcResult result = restActions
            .post("/payment-records", request)
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEqualTo("reportedDateOffline: must not be null");
    }

    @Test
    public void testBarclaycardPaymentWithEmptyReportedDateOfflineShouldFail() throws Exception {
        PaymentRecordRequest request = getBarclayCardPaymentRequest();
        request.setReportedDateOffline("");

        MvcResult result = restActions
            .post("/payment-records", request)
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEqualTo("validReportedDateOffline: Invalid payment reported offline date. Date format should be UTC.");
    }

    @Test
    public void testBarclaycardPaymentRecordRequestWithInvalidReportedDateOfflineShouldFail() throws Exception {
        PaymentRecordRequest request = getBarclayCardPaymentRequest();
        request.setReportedDateOffline("Invalid_date_string");

        MvcResult result = restActions
            .post("/payment-records", request)
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEqualTo("validReportedDateOffline: Invalid payment reported offline date. Date format should be UTC.");

    }

    private PaymentRecordRequest getCashPaymentRequest() {
        return PaymentRecordRequest.createPaymentRecordRequestDtoWith()
            .amount(new BigDecimal("32.19"))
            .paymentMethod(PaymentMethodType.CASH)
            .reference("ref_123")
            .externalProvider("middle office provider")
            .service(Service.DIGITAL_BAR)
            .currency(CurrencyCode.GBP)
            .giroSlipNo("12345")
            .reportedDateOffline(DateTime.now().toString())
            .siteId("AA99")
            .fees(
                Arrays.asList(
                    FeeDto.feeDtoWith()
                        .calculatedAmount(new BigDecimal("32.19"))
                        .code("FEE0123")
                        .memoLine("Bar Cash")
                        .naturalAccountCode("21245654433")
                        .version("1")
                        .volume(1d)
                        .reference("ref_123")
                        .build()
                )
            )
            .build();
    }

    private PaymentRecordRequest getChequePaymentRequest() {
        return PaymentRecordRequest.createPaymentRecordRequestDtoWith()
            .amount(new BigDecimal("99.99"))
            .paymentMethod(PaymentMethodType.CHEQUE)
            .reference("ref_122")
            .externalProvider("middle office provider")
            .service(Service.DIGITAL_BAR)
            .currency(CurrencyCode.GBP)
            .externalReference("1000012")
            .giroSlipNo("434567")
            .reportedDateOffline(DateTime.now().toString())
            .siteId("AA001")
            .fees(
                Arrays.asList(
                    FeeDto.feeDtoWith()
                        .calculatedAmount(new BigDecimal("99.99"))
                        .code("FEE0111")
                        .version("1")
                        .volume(1d)
                        .reference("ref_122")
                        .build()
                )
            )
            .build();
    }

    private PaymentRecordRequest getRequestWithNoCcdCaseNumberAndCaseReference() {
        return PaymentRecordRequest.createPaymentRecordRequestDtoWith()
            .amount(new BigDecimal("32.19"))
            .service(Service.DIGITAL_BAR)
            .currency(CurrencyCode.GBP)
            .giroSlipNo("12345")
            .reportedDateOffline(DateTime.now().toString())
            .siteId("AA99")
            .fees(
                Arrays.asList(
                    FeeDto.feeDtoWith()
                        .calculatedAmount(new BigDecimal("32.19"))
                        .code("FEE0123")
                        .memoLine("Bar Cash")
                        .naturalAccountCode("21245654433")
                        .version("1")
                        .volume(1d)
                        .build()
                )
            )
            .build();
    }

    private PaymentRecordRequest getPostalOrderPaymentRequest() {
        return PaymentRecordRequest.createPaymentRecordRequestDtoWith()
            .amount(new BigDecimal("99.99"))
            .paymentMethod(PaymentMethodType.POSTAL_ORDER)
            .reference("ref_122")
            .service(Service.DIGITAL_BAR)
            .currency(CurrencyCode.GBP)
            .externalProvider("middle office provider")
            .externalReference("postal_1000012")
            .giroSlipNo("434567")
            .reportedDateOffline(DateTime.now().toString())
            .siteId("AA001")
            .fees(
                Arrays.asList(
                    FeeDto.feeDtoWith()
                        .calculatedAmount(new BigDecimal("99.99"))
                        .code("FEE0111")
                        .reference("ref_122")
                        .version("1")
                        .volume(1d)
                        .build()
                )
            )
            .build();
    }

    private PaymentRecordRequest getBarclayCardPaymentRequest() {
        return PaymentRecordRequest.createPaymentRecordRequestDtoWith()
            .amount(new BigDecimal("99.99"))
            .paymentMethod(PaymentMethodType.CARD)
            .reference("ref_122")
            .service(Service.DIGITAL_BAR)
            .currency(CurrencyCode.GBP)
            .externalProvider("barclaycard")
            .externalReference("bar_card_1000013")
            .reportedDateOffline(DateTime.now().toString())
            .siteId("AA001")
            .fees(
                Arrays.asList(
                    FeeDto.feeDtoWith()
                        .calculatedAmount(new BigDecimal("99.99"))
                        .code("FEE0111")
                        .reference("ref_122")
                        .version("1")
                        .volume(1d)
                        .build()
                )
            )
            .build();
    }

    private PaymentRecordRequest getNoProviderPaymentRequest() {
        return PaymentRecordRequest.createPaymentRecordRequestDtoWith()
            .amount(new BigDecimal("99.99"))
            .paymentMethod(PaymentMethodType.CARD)
            .reference("ref_122")
            .service(Service.DIGITAL_BAR)
            .currency(CurrencyCode.GBP)
            .externalReference("bar_card_1000013")
            .reportedDateOffline(DateTime.now().toString())
            .siteId("AA001")
            .fees(
                Arrays.asList(
                    FeeDto.feeDtoWith()
                        .calculatedAmount(new BigDecimal("99.99"))
                        .code("FEE0111")
                        .reference("ref_122")
                        .version("1")
                        .volume(1d)
                        .build()
                )
            )
            .build();
    }

    private PaymentRecordRequest getBarclayCardPaymentRequestWithoutReportedDateOffline() {
        return PaymentRecordRequest.createPaymentRecordRequestDtoWith()
            .amount(new BigDecimal("99.99"))
            .paymentMethod(PaymentMethodType.CARD)
            .reference("ref_122")
            .service(Service.DIGITAL_BAR)
            .currency(CurrencyCode.GBP)
            .externalProvider("barclaycard")
            .externalReference("bar_card_1000013")
            .siteId("AA001")
            .fees(
                Arrays.asList(
                    FeeDto.feeDtoWith()
                        .calculatedAmount(new BigDecimal("99.99"))
                        .code("FEE0111")
                        .reference("ref_122")
                        .version("1")
                        .volume(1d)
                        .build()
                )
            )
            .build();
    }

    @Test
    public void testThatGivenARecordBarclaycardPaymentWhenItsFetchedThroughSlashPaymentsItContainsAReportedDateOffline() throws Exception {
        PaymentRecordRequest request = getBarclayCardPaymentRequest();

        MvcResult result = restActions
            .post("/payment-records", request)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto response = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertThat(response.getPaymentGroupReference()).isNotNull();
        assertThat(response.getReference().matches(PAYMENT_REFERENCE_REFEX)).isEqualTo(true);
        assertThat(response.getStatus()).isEqualTo("Initiated");

        restActions
            .post("/api/ff4j/store/features/payment-search/enable")
            .andExpect(status().isAccepted());

        MvcResult result2 = restActions
            .get("/payments?service_name=" + Service.DIGITAL_BAR)
            .andExpect(status().isOk())
            .andReturn();

        PaymentsResponse response2 = objectMapper.readValue(result2.getResponse().getContentAsByteArray(), PaymentsResponse.class);

        assertThat(response2.getPayments().size()).isGreaterThan(0);

        assertThat(response2.getPayments().get(0).getReportedDateOffline()).isNotNull();
        assertThat(response2.getPayments().get(0).getReportedDateOffline()).isNotBlank();
        assertThat(response2.getPayments().get(0).getReportedDateOffline()).isNotEmpty();

    }

}
