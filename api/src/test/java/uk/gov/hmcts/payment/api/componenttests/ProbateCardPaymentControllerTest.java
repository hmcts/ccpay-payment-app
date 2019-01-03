package uk.gov.hmcts.payment.api.componenttests;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;

import java.math.BigDecimal;
import java.util.Arrays;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static uk.gov.hmcts.payment.api.model.PaymentFeeLink.paymentFeeLinkWith;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@Transactional
public class ProbateCardPaymentControllerTest extends PaymentsDataUtil {

    private final static String PAYMENT_REFERENCE_REGEX = "^[RC-]{3}(\\w{4}-){3}(\\w{4}){1}";

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(9190);

    @Rule
    public WireMockClassRule instanceRule = wireMockRule;



    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    private PaymentDbBackdoor db;


    private RestActions restActions;

    @Autowired
    private ObjectMapper objectMapper;
    @Before
    public void setup() {
        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, null, objectMapper);

        restActions
            .withAuthorizedService("divorce")
            .withReturnUrl("https://www.gooooogle.com");

    }


    @Test
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
            .post("/probate-card-payments", cardPaymentRequest())
            .andExpect(status().isCreated())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);

        MvcResult result2 = restActions
            .get("/probate-card-payments/" + paymentDto.getReference())
            .andExpect(status().isOk())
            .andReturn();

        PaymentDto paymentsResponse = objectMapper.readValue(result2.getResponse().getContentAsString(), PaymentDto.class);

        assertEquals("http://payments.com", db.findByReference(paymentsResponse.getPaymentGroupReference()).getPayments().get(0).getServiceCallbackUrl());

        assertNotNull(paymentDto);
        assertEquals("Initiated", paymentDto.getStatus());
        assertTrue(paymentDto.getReference().matches(PAYMENT_REFERENCE_REGEX));
    }

    @Test
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
            .post("/probate-card-payments", cardPaymentRequest)
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        assertEquals(result.getResponse().getContentAsString(), "eitherOneRequired: Either ccdCaseNumber or caseReference is required.");
    }

    @Test
    public void retrieveCardPaymentAndMapTheGovPayStatusTest() throws Exception {
        stubFor(get(urlPathMatching("/v1/payments/ia2mv22nl5o880rct0vqfa7k76"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(contentsOf("gov-pay-responses/get-payment-response.json"))));

        //Create a payment in db
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("11.99"))
            .caseReference("Reference1")
            .ccdCaseNumber("ccdCaseNumber1")
            .description("Description1")
            .serviceType("PROBATE")
            .currency("GBP")
            .siteId("AA01")
            .userId(null)
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
            .get("/probate-card-payments/" + savedPayment.getReference())
            .andExpect(status().isOk())
            .andReturn();

        PaymentDto paymentDto = objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertNotNull(paymentDto);
        assertEquals(paymentDto.getReference(), payment.getReference());
        assertEquals(paymentDto.getExternalReference(), payment.getExternalReference());
        assertEquals("Success", paymentDto.getStatus());
    }


    @Test
    public void createCardPaymentWithInvalidInputDataShouldReturnStatusBadRequestTest() throws Exception {
        restActions
            .withReturnUrl("https://www.google.com")
            .post("/probate-card-payments", cardPaymentInvalidRequestJson())
            .andExpect(status().isBadRequest());
    }
    private CardPaymentRequest cardPaymentRequest() throws Exception {
        return objectMapper.readValue(requestJson().getBytes(), CardPaymentRequest.class);
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
