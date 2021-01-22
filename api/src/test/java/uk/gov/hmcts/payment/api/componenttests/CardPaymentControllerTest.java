package uk.gov.hmcts.payment.api.componenttests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.apache.commons.lang3.RandomUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.configuration.SecurityUtils;
import uk.gov.hmcts.payment.api.configuration.security.ServiceAndUserAuthFilter;
import uk.gov.hmcts.payment.api.configuration.security.ServicePaymentFilter;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.external.client.dto.CardDetails;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.CustomResultMatcher;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static uk.gov.hmcts.payment.api.configuration.security.ServiceAndUserAuthFilterTest.getUserInfoBasedOnUID_Roles;
import static uk.gov.hmcts.payment.api.model.PaymentFeeLink.paymentFeeLinkWith;
@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@EnableFeignClients
@AutoConfigureMockMvc
@Transactional
public class CardPaymentControllerTest extends PaymentsDataUtil {

    private final static String PAYMENT_REFERENCE_REGEX = "^[RC-]{3}(\\w{4}-){3}(\\w{4})";

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(9190);

    @Rule
    public WireMockClassRule instanceRule = wireMockRule;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ServiceAuthFilter serviceAuthFilter;

    @InjectMocks
    private ServiceAndUserAuthFilter serviceAndUserAuthFilter;

    @MockBean
    private SecurityUtils securityUtils;

    @Autowired
    private ServicePaymentFilter servicePaymentFilter;

    @Autowired
    private PaymentDbBackdoor db;


    private RestActions restActions;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LaunchDarklyFeatureToggler featureToggler;

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

    }

    @Test
    @WithMockUser(authorities = "payments")
    @Transactional
    public void createCardPaymentWithValidInputData_shouldReturnStatusCreatedTest() throws Exception {

        stubFor(post(urlPathMatching("/v1/payments"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(contentsOf("gov-pay-responses/create-payment-response.json"))));

        stubFor(get(urlPathMatching("/v1/payments/ak8gtvb438drmp59cs7ijppr3i"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(contentsOf("gov-pay-responses/get-payment-response.json"))));


        MvcResult result = restActions
            .withReturnUrl("https://www.google.com")
            .withHeader("service-callback-url", "http://payments.com")
            .post("/card-payments", cardPaymentRequest())
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        MvcResult result2 = restActions
            .get("/card-payments/" + paymentDto.getReference())
            .andExpect(status().isOk())
            .andReturn();

        PaymentDto paymentsResponse = objectMapper.readValue(result2.getResponse().getContentAsString(), PaymentDto.class);

        assertEquals("http://payments.com", db.findByReference(paymentsResponse.getPaymentGroupReference()).getPayments().get(0).getServiceCallbackUrl());

        assertNotNull(paymentDto);
        assertEquals("Initiated", paymentDto.getStatus());
        assertTrue(paymentDto.getReference().matches(PAYMENT_REFERENCE_REGEX));
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void createCardPaymentWithInvalidInputDataShouldReturnStatusBadRequestTest() throws Exception {
        restActions
            .withReturnUrl("https://www.google.com")
            .post("/card-payments", cardPaymentInvalidRequestJson())
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void createCardPayment_withMissingCcdCaseNumberAndCaseReference_shouldReturn422Test() throws Exception {
        CardPaymentRequest cardPaymentRequest = CardPaymentRequest.createCardPaymentRequestDtoWith()
            .amount(new BigDecimal("200.11"))
            .currency(CurrencyCode.GBP)
            .description("Test cross field validation")
            .service(Service.CMC)
            .siteId("siteID")
            .fees(Arrays.asList(FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("200.11"))
                .code("X0001")
                .version("1")
                .build())).build();


        MvcResult result = restActions
            .post("/card-payments", cardPaymentRequest)
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        assertEquals(result.getResponse().getContentAsString(), "eitherOneRequired: Either ccdCaseNumber or caseReference is required.");
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void retrieveCardPaymentAndMapTheGovPayStatusTest() throws Exception {
        stubFor(get(urlPathMatching("/v1/payments/ia2mv22nl5o880rct0vqfa7k76"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(contentsOf("gov-pay-responses/get-payment-response.json"))));

        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("11.99"))
            .caseReference("Reference1")
            .ccdCaseNumber("ccdCaseNumber1")
            .description("Description1")
            .serviceType("PROBATE")
            .currency("GBP")
            .siteId("AA01")
            .userId("1")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .paymentProvider(PaymentProvider.paymentProviderWith().name("gov pay").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .externalReference("ia2mv22nl5o880rct0vqfa7k76")
            .reference("RC-1519-9028-1909-3475")
            .statusHistories(Arrays.asList(StatusHistory.statusHistoryWith()
                .status("Initiated")
                .externalStatus("created")
                .build()))
            .build();
        PaymentFee fee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("11.99")).version("1").code("X0001").build();

        PaymentFeeLink paymentFeeLink = db.create(paymentFeeLinkWith().paymentReference("2018-15186162001").payments(Arrays.asList(payment)).fees(Arrays.asList(fee)));
        payment.setPaymentLink(paymentFeeLink);

        Payment savedPayment = paymentFeeLink.getPayments().get(0);

        MvcResult result = restActions
            .get("/card-payments/" + savedPayment.getReference())
            .andExpect(status().isOk())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
        assertEquals(paymentDto.getReference(), payment.getReference());
        assertEquals(paymentDto.getExternalReference(), payment.getExternalReference());
        assertEquals("Success", paymentDto.getStatus());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void retrieveCardPaymentStatuses_byPaymentReferenceTest() throws Exception {
        stubFor(get(urlPathMatching("/v1/payments/e2kkddts5215h9qqoeuth5c0v3"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(contentsOf("gov-pay-responses/get-payment-status-response.json"))));

        StatusHistory statusHistory = StatusHistory.statusHistoryWith().status("Initiated").externalStatus("created").build();
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("499.99"))
            .caseReference("Reference1")
            .ccdCaseNumber("ccdCaseNumber1")
            .description("Test payments statuses")
            .serviceType("PROBATE")
            .currency("GBP")
            .siteId("AA01")
            .userId("1")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .paymentProvider(PaymentProvider.paymentProviderWith().name("gov pay").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .externalReference("e2kkddts5215h9qqoeuth5c0v3")
            .reference("RC-1519-9028-2432-9115")
            .statusHistories(Arrays.asList(statusHistory))
            .build();
        PaymentFee fee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("499.99")).version("1").code("X0123").build();

        PaymentFeeLink paymentFeeLink = db.create(paymentFeeLinkWith().paymentReference("2018-15186162002").payments(Arrays.asList(payment)).fees(Arrays.asList(fee)));
        payment.setPaymentLink(paymentFeeLink);

        Payment savedPayment = paymentFeeLink.getPayments().get(0);

        MvcResult result = restActions
            .get("/card-payments/" + savedPayment.getReference() + "/statuses")
            .andExpect(status().isOk())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
        assertEquals(paymentDto.getReference(), savedPayment.getReference());
        assertEquals(paymentDto.getAmount(), new BigDecimal("499.99"));
        assertEquals(paymentDto.getStatusHistories().size(), 1);
        paymentDto.getStatusHistories().stream().forEach(h -> {
            assertEquals(h.getStatus(), "Success");
            assertEquals(h.getExternalStatus(), "success");
        });
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void retrieveCardDetails_byPaymentReferenceTest() throws Exception {
        stubFor(get(urlPathMatching("/v1/payments/ah0288ctvgqgcmbatdp1viu61j"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(contentsOf("gov-pay-responses/get-card-details-response.json"))));

        StatusHistory statusHistory = StatusHistory.statusHistoryWith().status("Success").externalStatus("success").build();
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("121.11"))
            .caseReference("Reference")
            .ccdCaseNumber("ccdCaseNumber")
            .description("Test payments statuses")
            .serviceType("PROBATE")
            .currency("GBP")
            .siteId("AA011")
            .userId("1")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .paymentProvider(PaymentProvider.paymentProviderWith().name("gov pay").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
            .externalReference("ah0288ctvgqgcmbatdp1viu61j")
            .reference("RC-1529-9159-9129-3183")
            .statusHistories(Arrays.asList(statusHistory))
            .build();
        PaymentFee fee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("121.11")).version("1").code("FEE0123").build();

        PaymentFeeLink paymentFeeLink = db.create(paymentFeeLinkWith().paymentReference("2018-15186161221").payments(Arrays.asList(payment)).fees(Arrays.asList(fee)));
        payment.setPaymentLink(paymentFeeLink);

        Payment savedPayment = paymentFeeLink.getPayments().get(0);

        MvcResult result = restActions
            .get("/card-payments/RC-1529-9159-9129-3183/details")
            .andExpect(status().isOk())
            .andReturn();

        CardDetails cardDetails = objectMapper.readValue(result.getResponse().getContentAsByteArray(), CardDetails.class);
        assertEquals(cardDetails.getExpiryDate(), "11/18");
        assertEquals(cardDetails.getCardBrand(), "Visa");
        assertEquals(cardDetails.getCardholderName(), "TEST CARD");
        assertEquals(cardDetails.getLastDigitsCardNumber(), "1111");

    }

    @Test
    @WithMockUser(authorities = "payments")
    public void retrieveCardDetails_shouldReturn404_ifDetailsNotFoundTest() throws Exception {
        stubFor(get(urlPathMatching("/v1/payments/ia2mv22nl5o880rct0vqfa7k76"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(contentsOf("gov-pay-responses/get-payment-error-response.json"))));

        StatusHistory statusHistory = StatusHistory.statusHistoryWith().status("Failed").externalStatus("error").build();
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("22.89"))
            .caseReference("Reference")
            .ccdCaseNumber("ccdCaseNumber")
            .description("Test payments statuses")
            .serviceType("PROBATE")
            .currency("GBP")
            .siteId("AA001")
            .userId("1")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .paymentProvider(PaymentProvider.paymentProviderWith().name("gov pay").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("error").build())
            .externalReference("ia2mv22nl5o880rct0vqfa7k76")
            .reference("RC-1518-9429-1432-7825")
            .statusHistories(Arrays.asList(statusHistory))
            .build();
        PaymentFee fee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("22.89")).version("1").code("FEE0112").build();

        PaymentFeeLink paymentFeeLink = db.create(paymentFeeLinkWith().paymentReference("2018-15186161221").payments(Arrays.asList(payment)).fees(Arrays.asList(fee)));
        payment.setPaymentLink(paymentFeeLink);

        Payment savedPayment = paymentFeeLink.getPayments().get(0);

        restActions
            .get("/card-payments/RC-1518-9429-1432-7825/details")
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void createPaymentWithChannelTelephonyAndProviderPciPal() throws Exception {
        BigDecimal amount = new BigDecimal("100");
        CardPaymentRequest cardPaymentRequest = CardPaymentRequest.createCardPaymentRequestDtoWith()
            .amount(amount)
            .description("description")
            .caseReference("telRefNumber")
            .ccdCaseNumber("1234")
            .service(Service.PROBATE)
            .currency(CurrencyCode.GBP)
            .provider("pci pal")
            .channel("telephony")
            .siteId("siteId")
            .fees(Collections.singletonList(FeeDto.feeDtoWith()
                .code("feeCode")
                .version("1")
                .calculatedAmount(new BigDecimal("100.1"))
                .build()))
            .build();

        MvcResult result = restActions
            .withReturnUrl("https://www.google.com")
            .withHeader("service-callback-url", "http://payments.com")
            .post("/card-payments", cardPaymentRequest)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        MvcResult result2 = restActions
            .get("/card-payments/" + paymentDto.getReference())
            .andExpect(status().isOk())
            .andReturn();

        PaymentDto paymentsResponse = objectMapper.readValue(result2.getResponse().getContentAsString(), PaymentDto.class);

        assertEquals("http://payments.com", db.findByReference(paymentsResponse.getPaymentGroupReference()).getPayments().get(0).getServiceCallbackUrl());

        assertNotNull(paymentDto);
        assertEquals("Initiated", paymentDto.getStatus());
        assertTrue(paymentDto.getReference().matches(PAYMENT_REFERENCE_REGEX));
        assertEquals("Amount saved in remissionDbBackdoor is equal to the on inside the request", amount, paymentsResponse.getAmount());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void retrieveCardPayment_withNonExistingReferenceTest() throws Exception {
        restActions
            .get("/card-payments/" + "RC-1518-9576-1498-8035")
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void retrieveCardPayment_andMapGovPayErrorStatusTest() throws Exception {
        stubFor(get(urlPathMatching("/v1/payments/ia2mv22nl5o880rct0vqfa7k76"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(contentsOf("gov-pay-responses/get-payment-error-response.json"))));

        StatusHistory statusHistory = StatusHistory.statusHistoryWith().status("Initiated").externalStatus("created").build();
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("22.89"))
            .caseReference("Reference")
            .ccdCaseNumber("ccdCaseNumber")
            .description("Description")
            .serviceType("PROBATE")
            .currency("GBP")
            .siteId("AA00")
            .userId("1")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .paymentProvider(PaymentProvider.paymentProviderWith().name("gov pay").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .externalReference("ia2mv22nl5o880rct0vqfa7k76")
            .reference("RC-1518-9429-1432-7825")
            .statusHistories(Arrays.asList(statusHistory))
            .build();
        PaymentFee fee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("22.89")).version("1").code("X0011").build();

        PaymentFeeLink paymentFeeLink = db.create(paymentFeeLinkWith().paymentReference("2018-15186162003").payments(Arrays.asList(payment)).fees(Arrays.asList(fee)));
        payment.setPaymentLink(paymentFeeLink);

        Payment savedPayment = paymentFeeLink.getPayments().get(0);

        MvcResult result = restActions
            .get("/card-payments/" + savedPayment.getReference())
            .andExpect(status().isOk())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
        assertEquals(paymentDto.getReference(), payment.getReference());
        assertEquals(paymentDto.getStatus(), "Failed");

        PaymentFeeLink savedPaymentGroup = db.findByReference("2018-15186162003");
        Payment dbPayment = savedPaymentGroup.getPayments().get(0);
        assertEquals(dbPayment.getReference(), "RC-1518-9429-1432-7825");
        dbPayment.getStatusHistories().stream().forEach(h -> {
            assertEquals(h.getErrorCode(), "P0200");
            assertEquals(h.getMessage(), "Payment not found");
        });
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void createCardPaymentForCMC_withCaseReferenceOnly_shouldReturnStatusCreatedTest() throws Exception {

        stubFor(post(urlPathMatching("/v1/payments"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(contentsOf("gov-pay-responses/create-payment-response.json"))));


        MvcResult result = restActions
            .withReturnUrl("https://www.google.com")
            .post("/card-payments", cardPaymentRequestWithCaseReference())
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
        assertEquals("Initiated", paymentDto.getStatus());
        assertTrue(paymentDto.getReference().matches(PAYMENT_REFERENCE_REGEX));
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void creatingCardPaymentWithCcdCaseNumberInsideFeeGetsSavedProperly() throws Exception {
        String testCcdCaseNumber = "test_case_number_1234";
        CardPaymentRequest cardPaymentRequest = CardPaymentRequest.createCardPaymentRequestDtoWith()
            .amount(new BigDecimal("200.11"))
            .currency(CurrencyCode.GBP)
            .description("Test cross field validation")
            .service(Service.CMC)
            .siteId("siteID")
            .ccdCaseNumber(testCcdCaseNumber)
            .provider("pci pal")
            .channel("telephony")
            .fees(Arrays.asList(FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("200.11"))
                .code("X0001")
                .version("1")
                .ccdCaseNumber(testCcdCaseNumber)
                .build())).build();


        MvcResult result = restActions
            .post("/card-payments", cardPaymentRequest)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
        assertEquals("Ccd case number inside fee is correct in the response", testCcdCaseNumber, paymentDto.getFees().get(0).getCcdCaseNumber());
        PaymentFeeLink paymentFeeLink = db.findByReference(paymentDto.getPaymentGroupReference());
        assertNotNull(paymentFeeLink);
        assertEquals("Ccd case number inside fee is correct taken from DB", testCcdCaseNumber, paymentFeeLink.getFees().get(0).getCcdCaseNumber());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void creatingCardPaymentWithCcdCaseNumberOnPaymentLevelOnlySavesCcdCaseNumberInsideFees() throws Exception {
        String testCcdCaseNumber = "test_case_number_1234";
        CardPaymentRequest cardPaymentRequest = CardPaymentRequest.createCardPaymentRequestDtoWith()
            .amount(new BigDecimal("200.11"))
            .currency(CurrencyCode.GBP)
            .description("Test cross field validation")
            .service(Service.CMC)
            .siteId("siteID")
            .ccdCaseNumber(testCcdCaseNumber)
            .provider("pci pal")
            .channel("telephony")
            .fees(Arrays.asList(FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("200.11"))
                .code("X0001")
                .version("1")
                .build())).build();


        MvcResult result = restActions
            .post("/card-payments", cardPaymentRequest)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
        assertEquals("Ccd case number inside fee is correct in the response", testCcdCaseNumber, paymentDto.getFees().get(0).getCcdCaseNumber());
        PaymentFeeLink paymentFeeLink = db.findByReference(paymentDto.getPaymentGroupReference());
        assertNotNull(paymentFeeLink);
        assertEquals("Ccd case number inside fee is correct taken from DB", testCcdCaseNumber, paymentFeeLink.getFees().get(0).getCcdCaseNumber());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void creatingCardPaymentWithCcdCaseNumberOnPaymentLevelOnlySavesCcdCaseNumberInsideFeesAndDoesNotOverwriteAlreadySetCcdCaseNumberInFee() throws Exception {
        String testCcdCaseNumber = "test_case_number_1234";
        String testCcdCaseNumber2 = "test_case_number_4321";
        CardPaymentRequest cardPaymentRequest = CardPaymentRequest.createCardPaymentRequestDtoWith()
            .amount(new BigDecimal("200.11"))
            .currency(CurrencyCode.GBP)
            .description("Test cross field validation")
            .service(Service.CMC)
            .siteId("siteID")
            .ccdCaseNumber(testCcdCaseNumber)
            .provider("pci pal")
            .channel("telephony")
            .fees(Arrays.asList(FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("200.11"))
                .code("X0001")
                .version("1")
                .build(), FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("300.11"))
                .code("X0002")
                .ccdCaseNumber(testCcdCaseNumber2)
                .version("1")
                .build())).build();


        MvcResult result = restActions
            .post("/card-payments", cardPaymentRequest)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
        assertEquals("Ccd case number inside fee is correct in the response", testCcdCaseNumber, paymentDto.getFees().get(0).getCcdCaseNumber());
        PaymentFeeLink paymentFeeLink = db.findByReference(paymentDto.getPaymentGroupReference());
        assertNotNull(paymentFeeLink);
        assertEquals("Ccd case number inside fee is correct taken from DB", testCcdCaseNumber, paymentFeeLink.getFees().get(0).getCcdCaseNumber());
        assertEquals("Ccd case number inside fee is correct taken from DB", testCcdCaseNumber2, paymentFeeLink.getFees().get(1).getCcdCaseNumber());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void creatingCardPaymentWithoutFees() throws Exception {
        String testccdCaseNumber = "1212-2323-3434-5454";
        CardPaymentRequest cardPaymentRequest = CardPaymentRequest.createCardPaymentRequestDtoWith()
            .amount(new BigDecimal("200.11"))
            .currency(CurrencyCode.GBP)
            .description("Test cross field validation")
            .service(Service.CMC)
            .siteId("siteID")
            .ccdCaseNumber(testccdCaseNumber)
            .provider("pci pal")
            .channel("telephony")
            .build();


        MvcResult result = restActions
            .post("/card-payments", cardPaymentRequest)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
        PaymentFeeLink paymentFeeLink = db.findByReference(paymentDto.getPaymentGroupReference());
        assertNotNull(paymentFeeLink);
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void creatingCardPaymentWithNullFees() throws Exception {
        String testccdCaseNumber = "1212-2323-3434-5454";
        CardPaymentRequest cardPaymentRequest = CardPaymentRequest.createCardPaymentRequestDtoWith()
            .amount(new BigDecimal("200.11"))
            .currency(CurrencyCode.GBP)
            .description("Test cross field validation")
            .service(Service.CMC)
            .siteId("siteID")
            .ccdCaseNumber(testccdCaseNumber)
            .provider("pci pal")
            .channel("telephony")
            .fees(null)
            .build();


        MvcResult result = restActions
            .post("/card-payments", cardPaymentRequest)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
        PaymentFeeLink paymentFeeLink = db.findByReference(paymentDto.getPaymentGroupReference());
        assertEquals(paymentDto.getStatus() , "Initiated");
        assertNotNull(paymentFeeLink);
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void creatingCardPaymentWithWelshLanguage() throws Exception {
        stubFor(post(urlPathMatching("/v1/payments"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(contentsOf("gov-pay-responses/create-payment-response-welsh-language.json"))));

        String testccdCaseNumber = "1212-1234-1111-2222";
        CardPaymentRequest cardPaymentRequest = CardPaymentRequest.createCardPaymentRequestDtoWith()
            .amount(new BigDecimal("200.11"))
            .currency(CurrencyCode.GBP)
            .description("Test Welsh Language support")
            .service(Service.CMC)
            .siteId("siteID")
            .ccdCaseNumber(testccdCaseNumber)
            .provider("gov pay")
            .channel("telephony")
            .fees(Arrays.asList(FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("200.11"))
                .code("X0001")
                .version("1")
                .build()))
            .language("CY")
            .build();


        MvcResult result = restActions
            .post("/card-payments", cardPaymentRequest)
            .andExpect(status().isCreated())
            .andReturn();
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void createCardPayment_InvalidLanguageAttribute_shouldReturn422Test() throws Exception {
        CardPaymentRequest cardPaymentRequest = CardPaymentRequest.createCardPaymentRequestDtoWith()
            .amount(new BigDecimal("200.11"))
            .ccdCaseNumber("1234-1234-1234-1234")
            .currency(CurrencyCode.GBP)
            .description("Test Language validation Checks")
            .service(Service.CMC)
            .siteId("siteID")
            .fees(Arrays.asList(FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("200.11"))
                .code("X0001")
                .version("1")
                .build()))
            .language("GBR")
            .build();


        MvcResult result = restActions
            .post("/card-payments", cardPaymentRequest)
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        assertEquals(result.getResponse().getContentAsString(), "validLanguage: Invalid value for language attribute.");
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void cancelPayment_withFeatureFlagDisabled_shouldReturnValidMessage() throws Exception {
        restActions
            .post("/api/ff4j/store/features/payment-cancel/disable","")
            .andExpect(status().isAccepted());

        MvcResult result = createMockPayment();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        result = restActions.post("/card-payments/" + paymentDto.getReference() + "/cancel","")
            .andExpect(status().isBadRequest())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEqualTo("Payment cancel feature is not available for usage.");
    }

    @Test
    @WithMockUser(authorities = "payments")
    @Transactional
    public void cancelPaymentSuccess_shouldReturn204Test() throws Exception {
        restActions
            .post("/api/ff4j/store/features/payment-cancel/enable","")
            .andExpect(status().isAccepted());
        MvcResult result = createMockPayment();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        stubFor(post(urlPathMatching("/v1/payments/" + paymentDto.getExternalReference() + "/cancel"))
            .willReturn(aResponse()
                .withStatus(204)
                .withHeader("Content-Type", "application/json")));

        restActions.post("/card-payments/" + paymentDto.getReference() + "/cancel","")
            .andExpect(status().isNoContent())
            .andReturn();
    }

    @Test
    @WithMockUser(authorities = "payments")
    @Transactional
    public void cancelPaymentBadRequest_shouldReturn400Test() throws Exception {
        restActions
            .post("/api/ff4j/store/features/payment-cancel/enable","")
            .andExpect(status().isAccepted());
        MvcResult result = createMockPayment();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        stubFor(post(urlPathMatching("/v1/payments/" + paymentDto.getExternalReference() + "/cancel"))
            .willReturn(aResponse()
                .withStatus(204)
                .withHeader("Content-Type", "application/json")));

        restActions.post("/card-payments/" + paymentDto.getReference() + "/cancel","")
            .andExpect(status().isNoContent())
            .andReturn();

        stubFor(post(urlPathMatching("/v1/payments/" + paymentDto.getExternalReference() + "/cancel"))
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"code\":\"P0501\",\"description\":\"Cancellation of payment failed\"}")));

        MvcResult result2 = restActions
            .post("/card-payments/" + paymentDto.getReference() + "/cancel","")
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    @WithMockUser(authorities = "payments")
    @Transactional
    public void cancelPaymentIncorrectPaymentRef_shouldReturn404Test() throws Exception {
        restActions
            .post("/api/ff4j/store/features/payment-cancel/enable","")
            .andExpect(status().isAccepted());
        MvcResult result = createMockPayment();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        stubFor(post(urlPathMatching("/v1/payments/" + paymentDto.getExternalReference() + "/cancel"))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"code\":\"P0200\",\"description\":\"Govpay Payment Not Found\"}")));

        restActions.post("/card-payments/" + paymentDto.getReference() + "/cancel","")
            .andExpect(status().isNotFound())
            .andReturn();
    }

    @Test
    @WithMockUser(authorities = "payments")
    @Transactional
    public void cancelPaymentInternalServerError_shouldReturn500Test() throws Exception {
        restActions
            .post("/api/ff4j/store/features/payment-cancel/enable","")
            .andExpect(status().isAccepted());
        MvcResult result = createMockPayment();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        stubFor(post(urlPathMatching("/v1/payments/" + paymentDto.getExternalReference() + "/cancel"))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"code\":\"P0198\",\"description\":\"GovPayDownstreamSystemErrorException\"}")));

        restActions.post("/card-payments/" + paymentDto.getReference() + "/cancel","")
            .andExpect(status().isInternalServerError())
            .andReturn();
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void createCardPaymentWithMultipleFee_ExactPayment() throws Exception {

        String ccdCaseNumber = "1111CC12" + RandomUtils.nextInt();

        when(featureToggler.getBooleanValue("apportion-feature",false)).thenReturn(true);

        stubFor(post(urlPathMatching("/v1/payments"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(contentsOf("gov-pay-responses/create-payment-response-apportion.json"))));

        List<FeeDto> fees = new ArrayList<>();
        fees.add(FeeDto.feeDtoWith().code("FEE0271").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(20))
            .volume(1).version("1").calculatedAmount(new BigDecimal(20)).build());
        fees.add(FeeDto.feeDtoWith().code("FEE0271").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(40))
            .volume(1).version("1").calculatedAmount(new BigDecimal(40)).build());
        fees.add(FeeDto.feeDtoWith().code("FEE0271").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(60))
            .volume(1).version("1").calculatedAmount(new BigDecimal(60)).build());

        CardPaymentRequest cardPaymentRequest = CardPaymentRequest.createCardPaymentRequestDtoWith()
            .amount(new BigDecimal("120"))
            .description("description")
            .caseReference("telRefNumber")
            .ccdCaseNumber(ccdCaseNumber)
            .service(Service.PROBATE)
            .currency(CurrencyCode.GBP)
            .siteId("AA08")
            .fees(fees)
            .build();

        MvcResult result = restActions
            .withHeader("service-callback-url", "http://payments.com")
            .post("/card-payments", cardPaymentRequest)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        List<PaymentFee> savedfees = db.findByReference(paymentDto.getPaymentGroupReference()).getFees();

        assertEquals(new BigDecimal(20), savedfees.get(0).getAmountDue());
        assertEquals(new BigDecimal(40), savedfees.get(1).getAmountDue());
        assertEquals(new BigDecimal(60), savedfees.get(2).getAmountDue());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void createCardPaymentWithMultipleFee_ShortfallPayment() throws Exception {

        String ccdCaseNumber = "1111CC12" + RandomUtils.nextInt();

        when(featureToggler.getBooleanValue("apportion-feature",false)).thenReturn(true);

        stubFor(post(urlPathMatching("/v1/payments"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(contentsOf("gov-pay-responses/create-payment-response-apportion.json"))));

        List<FeeDto> fees = new ArrayList<>();
        fees.add(FeeDto.feeDtoWith().code("FEE0271").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(30))
            .volume(1).version("1").calculatedAmount(new BigDecimal(30)).build());
        fees.add(FeeDto.feeDtoWith().code("FEE0271").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(40))
            .volume(1).version("1").calculatedAmount(new BigDecimal(40)).build());
        fees.add(FeeDto.feeDtoWith().code("FEE0271").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(60))
            .volume(1).version("1").calculatedAmount(new BigDecimal(60)).build());

        CardPaymentRequest cardPaymentRequest = CardPaymentRequest.createCardPaymentRequestDtoWith()
            .amount(new BigDecimal("120"))
            .description("description")
            .caseReference("telRefNumber")
            .ccdCaseNumber(ccdCaseNumber)
            .service(Service.PROBATE)
            .currency(CurrencyCode.GBP)
            .siteId("AA08")
            .fees(fees)
            .build();

        MvcResult result = restActions
            .withHeader("service-callback-url", "http://payments.com")
            .post("/card-payments", cardPaymentRequest)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        List<PaymentFee> savedfees = db.findByReference(paymentDto.getPaymentGroupReference()).getFees();

        assertEquals(new BigDecimal(30), savedfees.get(0).getAmountDue());
        assertEquals(new BigDecimal(40), savedfees.get(1).getAmountDue());
        assertEquals(new BigDecimal(60), savedfees.get(2).getAmountDue());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void createCardPaymentWithMultipleFee_SurplusPayment() throws Exception {

        String ccdCaseNumber = "1111CC12" + RandomUtils.nextInt();

        when(featureToggler.getBooleanValue("apportion-feature",false)).thenReturn(true);

        stubFor(post(urlPathMatching("/v1/payments"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(contentsOf("gov-pay-responses/create-payment-response-apportion.json"))));

        List<FeeDto> fees = new ArrayList<>();
        fees.add(FeeDto.feeDtoWith().code("FEE0271").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(10))
            .volume(1).version("1").calculatedAmount(new BigDecimal(10)).build());
        fees.add(FeeDto.feeDtoWith().code("FEE0271").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(40))
            .volume(1).version("1").calculatedAmount(new BigDecimal(40)).build());
        fees.add(FeeDto.feeDtoWith().code("FEE0271").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(60))
            .volume(1).version("1").calculatedAmount(new BigDecimal(60)).build());

        CardPaymentRequest cardPaymentRequest = CardPaymentRequest.createCardPaymentRequestDtoWith()
            .amount(new BigDecimal("120"))
            .description("description")
            .caseReference("telRefNumber")
            .ccdCaseNumber(ccdCaseNumber)
            .service(Service.PROBATE)
            .currency(CurrencyCode.GBP)
            .siteId("AA08")
            .fees(fees)
            .build();

        MvcResult result = restActions
            .withHeader("service-callback-url", "http://payments.com")
            .post("/card-payments", cardPaymentRequest)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        List<PaymentFee> savedfees = db.findByReference(paymentDto.getPaymentGroupReference()).getFees();

        assertEquals(new BigDecimal(10), savedfees.get(0).getAmountDue());
        assertEquals(new BigDecimal(40), savedfees.get(1).getAmountDue());
        assertEquals(new BigDecimal(60), savedfees.get(2).getAmountDue());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void createCardPaymentWithMultipleFee_SurplusPayment_When_Apportion_Flag_Is_On() throws Exception {

        String ccdCaseNumber = "1111CC12" + RandomUtils.nextInt();
        when(featureToggler.getBooleanValue("apportion-feature",false)).thenReturn(true);
        stubFor(post(urlPathMatching("/v1/payments"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(contentsOf("gov-pay-responses/create-payment-response-apportion.json"))));

        List<FeeDto> fees = new ArrayList<>();
        fees.add(FeeDto.feeDtoWith().code("FEE0271").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(10))
            .volume(1).version("1").calculatedAmount(new BigDecimal(10)).build());
        fees.add(FeeDto.feeDtoWith().code("FEE0271").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(40))
            .volume(1).version("1").calculatedAmount(new BigDecimal(40)).build());
        fees.add(FeeDto.feeDtoWith().code("FEE0271").ccdCaseNumber(ccdCaseNumber).feeAmount(new BigDecimal(60))
            .volume(1).version("1").calculatedAmount(new BigDecimal(60)).build());

        CardPaymentRequest cardPaymentRequest = CardPaymentRequest.createCardPaymentRequestDtoWith()
            .amount(new BigDecimal("120"))
            .description("description")
            .caseReference("telRefNumber")
            .ccdCaseNumber(ccdCaseNumber)
            .service(Service.PROBATE)
            .currency(CurrencyCode.GBP)
            .siteId("AA08")
            .fees(fees)
            .build();

        MvcResult result = restActions
            .withHeader("service-callback-url", "http://payments.com")
            .post("/card-payments", cardPaymentRequest)
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        List<PaymentFee> savedfees = db.findByReference(paymentDto.getPaymentGroupReference()).getFees();

        assertEquals(new BigDecimal(10), savedfees.get(0).getAmountDue());
        assertEquals(new BigDecimal(40), savedfees.get(1).getAmountDue());
        assertEquals(new BigDecimal(60), savedfees.get(2).getAmountDue());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void retrieveCardPaymentStatuses_byInvalidPaymentReferenceTest() throws Exception {
        stubFor(get(urlPathMatching("/v1/payments/e2kkddts5215h9qqoeuth5c0v3"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(contentsOf("gov-pay-responses/get-payment-status-response.json"))));

        StatusHistory statusHistory = StatusHistory.statusHistoryWith().status("Initiated").externalStatus("created").build();
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("499.99"))
            .caseReference("Reference1")
            .ccdCaseNumber("ccdCaseNumber1")
            .description("Test payments statuses")
            .serviceType("PROBATE")
            .currency("GBP")
            .siteId("AA01")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .paymentProvider(PaymentProvider.paymentProviderWith().name("gov pay").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .externalReference("e2kkddts5215h9qqoeuth5c0v3")
            .reference("RC-1519-9028-2432-9115")
            .statusHistories(Arrays.asList(statusHistory))
            .build();
        PaymentFee fee = PaymentFee.feeWith().calculatedAmount(new BigDecimal("499.99")).version("1").code("X0123").build();

        PaymentFeeLink paymentFeeLink = db.create(paymentFeeLinkWith().paymentReference("2018-15186162002").payments(Arrays.asList(payment)).fees(Arrays.asList(fee)));
        payment.setPaymentLink(paymentFeeLink);

        Payment savedPayment = paymentFeeLink.getPayments().get(0);

        restActions
            .withReturnUrl("https://www.google.com")
            .get("/card-payments/" + "12345" + "/statuses")
            .andExpect(status().isNotFound())
            .andReturn();
    }

    private CardPaymentRequest cardPaymentRequest() throws Exception {
        return objectMapper.readValue(requestJson().getBytes(), CardPaymentRequest.class);
    }

    private CardPaymentRequest cardPaymentRequestWithCaseReference() throws Exception {
        return objectMapper.readValue(jsonWithCaseReference().getBytes(), CardPaymentRequest.class);
    }

    @NotNull
    private MvcResult createMockPayment() throws Exception {
        stubFor(post(urlPathMatching("/v1/payments"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(contentsOf("gov-pay-responses/create-payment-response.json"))));

        return restActions
            .withReturnUrl("https://www.google.com")
            .withHeader("service-callback-url", "http://payments.com")
            .post("/card-payments", cardPaymentRequest())
            .andExpect(status().isCreated())
            .andReturn();
    }


    public String jsonWithCaseReference() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"case_reference\": \"caseReference\",\n" +
            "  \"service\": \"CMC\",\n" +
            "  \"currency\": \"GBP\",\n" +
            "  \"return_url\": \"https://www.gooooogle.com\",\n" +
            "  \"site_id\": \"siteId\",\n" +
            "  \"fees\": [\n" +
            "    {\n" +
            "      \"calculated_amount\": 101.89,\n" +
            "      \"code\": \"X0101\",\n" +
            "      \"version\": \"1\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private String cardPaymentInvalidRequestJson() {
        return "{\n" +
            "  \"amount\": 101.89,\n" +
            "  \"description\": \"New passport application\",\n" +
            "  \"ccd_case_number\": \"CCD101\",\n" +
            "  \"case_reference\": \"12345\",\n" +
            "  \"service\": \"PROBATE\",\n" +
            "  \"currency\": \"GBP\",\n" +
            "  \"return_url\": \"https://www.gooooogle.com\",\n" +
            "  \"site_id\": \"AA101\",\n" +
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
