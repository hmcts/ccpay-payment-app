package uk.gov.hmcts.payment.functional.tokens;


import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;
import uk.gov.hmcts.payment.api.configuration.security.util.OneTimePasswordFactory;

import static io.restassured.RestAssured.*;

@TestComponent
public class ServiceTokenFactory {
    private static final Logger logger = LoggerFactory.getLogger(ServiceTokenFactory.class);

    @Value("${s2s.url:http://rpe-service-auth-provider-aat.service.core-compute-aat.internal}")
    private String baseUrl;

    private OneTimePasswordFactory otpFactory;

    public String validTokenForService(String microservice, String secret) {
        defaultParser = Parser.JSON;

        String otp = new OneTimePasswordFactory().validOneTimePassword(secret);

        String jwt = given()
            .body(getS2sRequestBody(microservice, otp))
            .header("Content-Type", "application/json")
            .baseUri(baseUrl)
            .post( "/lease")
            .body().asString();

        logger.info("S2S auth token generated successfully");
        return jwt;
    }

    private String getS2sRequestBody(String microservice, String otp) {
        JSONObject request = new JSONObject();
        try {
            request.put("microservice", microservice);
            request.put("oneTimePassword", otp);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return request.toString();
    }
}
