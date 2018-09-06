package uk.gov.hmcts.payment.functional;

import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import io.restassured.response.ResponseBody;
import org.apache.commons.lang.RandomStringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.functional.dsl.PaymentsV2TestDsl;
import uk.gov.hmcts.payment.functional.tokens.ServiceTokenFactory;
import uk.gov.hmcts.payment.functional.tokens.UserTokenFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.*;
import static org.junit.Assert.*;

public class CreateCardPaymentIntegrationTest extends IntegrationTestBase {

    @Autowired(required = true)
    private PaymentsV2TestDsl dsl;

    @Value("${test.url:http://localhost:8080}")
    private String baseURL;

    @Autowired
    private UserTokenFactory userTokenFactory;

    @Autowired
    private ServiceTokenFactory serviceTokenFactory;

    private static final String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static String cmcUserId = UUID.randomUUID().toString() + "@hmcts.net";

    private static String cmcUserPassword = RandomStringUtils.random(15, characters);


    @Test
    public void createCMCCardPaymentShoudReturn201() {
        proxy(localProxyHost, Integer.parseInt(localProxyPort));

        String userAuthToken = userTokenFactory.validTokenForUser(cmcUserId, cmcUserPassword, cmcUserRole, cmcUserGroup);
        String serviceAuthToken = serviceTokenFactory.validTokenForService(cmcServiceName, cmcSecret);

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", userAuthToken);
        headers.put("ServiceAuthorization", serviceAuthToken);
        headers.put("return-url", "https://www.google.com");

        String reference = given()
            .relaxedHTTPSValidation()
            .headers(headers)
            .header("Content-Type", "application/json")
            .body(getCardPaymentRequest())
            .when()
            .baseUri(baseURL)
            .post("/card-payments")
            .then()
            .statusCode(201)
            .extract()
            .path("reference");

        assertNotNull(reference);


//        CardPaymentRequest cardPaymentRequest = CardPaymentRequest.createCardPaymentRequestDtoWith()
//            .amount(new BigDecimal("123.11"))
//            .description("A functional test card payment")
//            .caseReference("REF_123")
//            .service(Service.CMC)
//            .currency(CurrencyCode.GBP)
//            .siteId("SITEID_123")
//            .fees(Arrays.asList(FeeDto.feeDtoWith()
//                .calculatedAmount(new BigDecimal("123.11"))
//                .code("FEE0123")
//                .reference("REF_123")
//                .version("1")
//                .volume(new Double("1"))
//                .build()))
//            .build();
//
//
//        dsl.given().userId(cmcUserId, cmcUserPassword, cmcUserRole, cmcUserGroup).serviceId(cmcServiceName, cmcSecret).returnUrl("https://www.google.com")
//            .when().createCardPayment(cardPaymentRequest)
//            .then().created(paymentDto -> {
//                Assert.assertEquals("payment status is properly set", "Initiated", paymentDto.getStatus());
//        });
    }

    private String getCardPaymentRequest() {
        JSONObject payment = new JSONObject();
        try {


            payment.put("amount", 123.11);
            payment.put("description", "A functional test card payment");
            payment.put("case_reference", "REF_123");
            payment.put("service", "CMC");
            payment.put("currency", "GBP");
            payment.put("site_id", "AA123");

            JSONArray fees = new JSONArray();
            JSONObject fee = new JSONObject();
            fee.put("calculated_amount", 123.11);
            fee.put("code", "FEE0123");
            fee.put("reference", "REF_123");
            fee.put("version", "1");
            fees.put(fee);

            payment.put("fees", fees);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return payment.toString();
    }

}
