package uk.gov.hmcts.payment.api.service;

import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class LiberataService {

    @Value("${liberata.oauth2.token.url}")
    private String tokenUrl;

    @Value("${liberata.oauth2.client.id}")
    private String clientId;

    @Value("${liberata.oauth2.client.secret}")
    private String clientSecret;

    @Value("${liberata.oauth2.username}")
    private String username;

    @Value("${liberata.oauth2.password}")
    private String password;

    @Autowired
    @Qualifier("restTemplateLiberata")
    private RestTemplate restTemplate;

    public String getAccessToken() {
        // Create the request body
        String body = String.format("grant_type=password&username=%s&password=%s&client_id=%s&client_secret=%s",
            username, password, clientId, clientSecret);

        // Set the headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Create the request entity
        HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);

        // Make the request
        ResponseEntity<JSONObject> response = restTemplate.exchange(
            tokenUrl, HttpMethod.POST, requestEntity, JSONObject.class);

        // Check the response status
        if (response.getStatusCode() == HttpStatus.OK) {
            JSONObject jsonObject = response.getBody();
            return jsonObject.getAsString("access_token");
        } else {
            throw new RuntimeException("Failed to obtain access token: " + response.getStatusCode());
        }
    }
}
