package uk.gov.hmcts.payment.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.dto.liberata.identity.TokenResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import uk.gov.hmcts.payment.api.dto.liberata.identity.LiberataDto;
import java.util.Collections;

@Service
public class LiberataIdentity {

    private TokenResponse cachedToken;

    @Autowired()
    @Qualifier("liberataRestTemplate")
    private RestTemplate liberataRestTemplate;

    private TokenResponse getToken() {
        if (cachedToken != null && !cachedToken.isExpired()) {
            return cachedToken;
        }
        cachedToken = fetchNewToken();
        return cachedToken;
    }

    @Cacheable(value = "liberataToken", sync = true)
    public TokenResponse getValidToken() {
        TokenResponse token = getToken();

        if (token.isExpired()) {
            token = refreshToken();
        }

        return token;
    }

    @CachePut(value = "liberataToken")
    public TokenResponse refreshToken() {
        return fetchNewToken();
    }


    private TokenResponse fetchNewToken() {
        // https://lascustomerportaluat.liberata.com/pba_api_uat/api/auth/token
        //https://lascustomerportal.liberata.com/pba_api/api/auth/token
        //https://lascustomerportaluat.liberata.com/pba_api_test/api/auth/token
        
        //already tried
        // https://lascustomerportaluat.liberata.com/pba_api_uat/api/auth/token
        //https://lascustomerportaluat.liberata.com/api/auth/token
        String url = "https://lascustomerportaluat.liberata.com/api/auth/token";


        LiberataDto payload = new LiberataDto();
        payload.setEmail("joe@bloggs.com");
        payload.setPassword("Password123");

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<LiberataDto> request =
            new HttpEntity<>(payload, headers);

        try
        {
            ResponseEntity<TokenResponse> response =
                liberataRestTemplate.postForEntity(url, request, TokenResponse.class);

            System.out.println("Error fetching token from Liberata: " + response);
            System.out.println("Error fetching token from Liberata: " + response.getBody());
            return new TokenResponse();

        } catch (Exception exception) {
            System.out.println("Error fetching token from Liberata: " + exception.getMessage());
            exception.printStackTrace();
            return new TokenResponse();
        }
    }
}
