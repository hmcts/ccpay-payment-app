package uk.gov.hmcts.payment.functional;

import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import org.apache.commons.lang.RandomStringUtils;
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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.*;

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

    private CardPaymentRequest validCardPaymentRequest = CardPaymentRequest.createCardPaymentRequestDtoWith()
        .amount(new BigDecimal("200.11"))
        .caseReference("caseReference")
        .currency(CurrencyCode.GBP)
        .description("Test cross field validation")
        .service(Service.CMC)
        .siteId("siteID")
        .fees(Arrays.asList(FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("200.11"))
            .code("FEE0123")
            .version("1")
            .volume(new Double(1))
            .build())).build();


    @Test
    public void createCMCCardPaymentShoudReturn201() {
        baseURI = baseURL;
        defaultParser = Parser.JSON;
        useRelaxedHTTPSValidation();

        System.out.println("Payment baseURI : " + baseURL);

        Map<String, String> headers = new HashMap<>();

        headers.put("Authorization", userTokenFactory.validTokenForUser(cmcUserId, cmcUserPassword, cmcUserRole, cmcUserGroup));
        headers.put("ServiceAuthorization", serviceTokenFactory.validTokenForService(cmcServiceName, cmcSecret));
        headers.put("return-url", "https://localhost");

        String reference = given()
            .contentType("application/json")
            .headers(headers)
            .body(validCardPaymentRequest)
            .post("/card-payments")
            .then()
            .statusCode(201)
            .extract()
            .path("reference");

        System.out.println("Payment reference: " + reference);

//        dsl.given().userId(cmcUserId, cmcUserPassword, cmcUserRole, cmcUserGroup).serviceId(cmcServiceName, cmcSecret).returnUrl("https://www.google.com")
//            .when().createCardPayment(validCardPaymentRequest)
//            .then().created(paymentDto -> {
//                Assert.assertEquals("payment status is properly set", "Initiated", paymentDto.getStatus());
//        });
    }
}
