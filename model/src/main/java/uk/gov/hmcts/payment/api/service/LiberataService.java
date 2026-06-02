package uk.gov.hmcts.payment.api.service;

import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.exceptions.LiberataServiceException;

import java.time.Instant;

@Service
public class LiberataService {


    @Value("${laravel.oauth2.token.url}")
    private String laravelTokenUrl;

    @Value("${laravel.oauth2.refresh.url}")
    private String laravelRefreshUrl;

    @Value("${laravel.oauth2.password}")
    private String laravelPassword;

    @Value("${laravel.oauth2.email.address}")
    private String laravelEmailAddress;


    @Autowired
    @Qualifier("restTemplateLaravel")
    private RestTemplate restTemplateLaravel;

    private final TokenStore tokenStore;

    @Autowired
    public LiberataService(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    private final Object updateTokenLock = new Object();


    /**
     * An access token is sourced in four possible scenarios:
     * - A new valid token retrieved when no prior token available.
     * - An existing valid token previously retrieved from OAuth server.
     * - A refreshed token is retrieved after token expiry but during grace period.
     * - A new valid token is retrieved after token has expired after grace period.
     * @return an access token.
     */
    public String getAccessToken() {
        TokenState tokenState = tokenStore.get();
        if (isTokenValid(tokenState)) {
            return tokenState.token();
        }

        synchronized (updateTokenLock) {
            // single-threaded double-check for valid token
            tokenState = tokenStore.get();
            if (isTokenValid(tokenState)) {
                return tokenState.token();
            }

            if (tokenState == null || tokenState.token() == null) {
                return getAccessTokenUsingCreds();
            }
            return refreshAccessToken();
        }
    }

    /**
     * Determines if the provided TokenState contains a current, valid token.
     * @param tokenState the tokenState to be examined.
     * @return true if token provided is valid.
     */
    private boolean isTokenValid(TokenState tokenState) {
        return tokenState != null
            && tokenState.token() != null
            && tokenState.expiresAtUtc().isAfter(Instant.now());
    }


    /**
     * Makes a call to Laravel to request a token using credentials.
     * If successful it retains and returns the received token.
     * @return New access token
     */
    private String getAccessTokenUsingCreds() {
        // Create the request body
        JSONObject bodyJson = new JSONObject();
        bodyJson.put("email", laravelEmailAddress);
        bodyJson.put("password", laravelPassword);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestEntity = new HttpEntity<>(bodyJson.toString(), headers);

        ResponseEntity<JSONObject> response = restTemplateLaravel.exchange(
            laravelTokenUrl, HttpMethod.POST, requestEntity, JSONObject.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            JSONObject jsonObject= response.getBody();
            if (jsonObject != null) {
                return retainAccessToken(jsonObject, "token.plainTextToken", "token.expires_at");
            } else {
                throw new LiberataServiceException("Empty access token body received when using credentials");
            }
        } else {
            throw new LiberataServiceException("Failed to obtain access token when using credentials: " + response.getStatusCode());
        }
    }

    /**
     * Makes a call to Laravel OAuth Server to request a refreshed token, using the current token.
     * If the current token is missing, a token is requested using credentials.
     * The response is then processed by the parseRefreshResponse() method.
     * @return see parseRefreshResponse
     */
    private String refreshAccessToken() {

        TokenState tokenState = tokenStore.get();
        if (tokenState == null || tokenState.token() == null) {
            return getAccessTokenUsingCreds();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(tokenState.token());

        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);

        ResponseEntity<JSONObject> response = restTemplateLaravel.exchange(
            laravelRefreshUrl, HttpMethod.POST, requestEntity, JSONObject.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            JSONObject jsonObject = response.getBody();
            if (jsonObject != null) {
                return parseRefreshResponse(jsonObject);
            } else {
                throw new LiberataServiceException("Empty access token body received when refreshing access token");
            }
        } else {
            throw new LiberataServiceException("Failed to obtain access token when refreshing access token: " + response.getStatusCode());
        }
    }

    /**
     * Parses the given JSONObject, containing the response to a token refresh request.
     * Depending on the response, one of three outcomes occur:
     * a. The current token has expired, so a new token is requested/returned using credentials
     * b. The current token is still valid, so it is returned.
     * c. The token was in a grace period, so the newly received token is retained and returned.
     * @param jsonObject - to be parsed.
     * @return the extracted token, according to the rules above.
     */
    private String parseRefreshResponse(JSONObject jsonObject) {
        String message = jsonObject.getAsString("message");
        if (message != null) {
            if (message.contains("Unauthenticated")) { //expired token
                return getAccessTokenUsingCreds();
            } else { // current valid token
                TokenState tokenState = tokenStore.get();
                return tokenState == null ? getAccessTokenUsingCreds() : tokenState.token();
            }
        } else { //was in grace period
            return retainAccessToken(jsonObject, "token", "expires_at");
        }
    }


    /**
     * Extracts the token and expiry Instant from the JSONObject, to retain in temporary memory store.
     * @param jsonObject - to be parsed
     * @param tokenKey - the key for the token value within the json tree
     * @param expireKey - the key for the expiry date time within the json tree
     * @return the newly stored access token.
     */
    private String retainAccessToken(JSONObject jsonObject, String tokenKey, String expireKey) {
        String tokenVal = jsonObject.getAsString(tokenKey);
        String expireVal = jsonObject.getAsString(expireKey);
        if (tokenVal == null || expireVal == null) {
            throw new LiberataServiceException("null access token / expiry found when retaining access token");
        }
        Instant expiresAtUtc = Instant.parse(jsonObject.getAsString(expireKey));
        tokenStore.set(new TokenState(tokenVal, expiresAtUtc));
        return tokenVal;
    }

}
