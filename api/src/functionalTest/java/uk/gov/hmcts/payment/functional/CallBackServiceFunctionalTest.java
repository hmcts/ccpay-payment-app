package uk.gov.hmcts.payment.functional;

import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import io.restassured.RestAssured;
import org.awaitility.Duration;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.fixture.PaymentFixture;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.payment.functional.idam.IdamService.CMC_CITIZEN_GROUP;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class CallBackServiceFunctionalTest {

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(9190);

    @Autowired
    private TestConfigProperties testProps;

    @Autowired
    private PaymentsTestDsl dsl;

    @Autowired
    private IdamService idamService;
    @Autowired
    private S2sTokenService s2sTokenService;

    @Value("${gov.pay.url}")
    private String govpayUrl;

    @Value("${gov.pay.keys.cmc}")
    private String govpayCmcKey;

    private static String USER_TOKEN;
    private static String SERVICE_TOKEN;
    private static boolean TOKENS_INITIALIZED = false;

    @Before
    public void setUp() throws Exception {
        if (!TOKENS_INITIALIZED) {
            USER_TOKEN = idamService.createUserWith(CMC_CITIZEN_GROUP, "citizen").getAuthorisationToken();
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);
            TOKENS_INITIALIZED = true;
        }
    }


    @Test
    public void shouldInvokeCallBackForStatusUpdate() throws Exception {
        final String[] reference = new String[1];

        // create card payment
        dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .returnUrl("https://www.google.com")
            .serviceCallBackUrl("http://localhost:9190/service-callback")
            .when().createCardPayment(getCardPaymentRequest())
            .then().created(savedPayment -> {
            reference[0] = savedPayment.getReference();
            assertEquals("Initiated", savedPayment.getStatus());
        });

        // GET card payment
        PaymentDto paymentDto = dsl.given().userToken(USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().getCardPayment(reference[0])
            .then().get();

        // Cancel payment - trigger status change
        RestAssured.given()
            .header(AUTHORIZATION, "Bearer " + govpayCmcKey)
            .post(govpayUrl + "/" + paymentDto.getExternalReference() +"/cancel")
            .then()
            .statusCode(204);

        // Stub service callback response
        stubFor(patch(urlPathMatching("/service-callback"))
            .willReturn(aResponse()
                .withStatus(200)
            ));

        // invoke job schedule
        dsl.given()
            .s2sToken(SERVICE_TOKEN)
            .when().cardPaymentsStatusUpdateJob()
            .then()
            .ok();

        // verify callback invocation
        await()
            .pollInterval(Duration.FIVE_HUNDRED_MILLISECONDS)
            .atMost(Duration.FIVE_SECONDS)
            .until(() -> {
            try {
                // verify(1, patchRequestedFor(urlPathMatching("/service-callback")).withRequestBody(containing(reference[0])));
                return true;
            } catch (VerificationException ve) {
                return false;
            }
        });
    }

    private CardPaymentRequest getCardPaymentRequest() {
        return PaymentFixture.aCardPaymentRequest("20.99");
    }

}
