package uk.gov.hmcts.payment.functional.tokens;


import io.restassured.parsing.Parser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;
import uk.gov.hmcts.payment.functional.IntegrationTestBase;

import static io.restassured.RestAssured.*;

@TestComponent
public class UserTokenFactory extends IntegrationTestBase {
    private static final Logger logger = LoggerFactory.getLogger(UserTokenFactory.class);

    @Value("${idam.api.url:http://idam-api.aat.platform.hmcts.net}")
    private String baseUrl;

    public String validTokenForUser(String userId, String password) {
        defaultParser = Parser.JSON;

        String jwt =  given()
            .relaxedHTTPSValidation()
            .urlEncodingEnabled(true)
            .param("username", userId)
            .param("password", password)
            .header("Accept", "application/json")
            .baseUri(baseUrl)
            .post("/loginUser")
            .then()
            .statusCode(200)
            .extract()
            .path("access_token");

        logger.info("User auth token generated successfully");
        return jwt;
    }

    public void setUpUser(String userId, String password, String role, String userGroup) {
        String request = getCreateUserRequestBody(userId, password, role, userGroup);

        given()
            .relaxedHTTPSValidation()
            .contentType("application/json")
            .body(request)
            .baseUri(baseUrl)
            .post("/testing-support/accounts")
            .then()
            .statusCode(201);

    }

    public void deleteUser(String userId) {
        given()
            .relaxedHTTPSValidation()
            .baseUri(baseUrl)
            .delete("/testing-support/accounts/" + userId);

    }

    private String getCreateUserRequestBody(String userId, String password, String role, String userGroup) {

        JSONObject body = new JSONObject();
        try {
            body.put("email", userId);
            body.put("forename", "payuserforename");
            body.put("password", password);
            body.put("surname", "payusersurname");

            //Add role
            JSONObject code = new JSONObject();
            code.put("code", role);
            JSONArray roles = new JSONArray();
            roles.put(code);
            body.put("roles", roles);

            //Add usergroup
            JSONObject group = new JSONObject();
            group.put("code", userGroup);
            body.put("userGroup", group);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return body.toString();
    }

}
