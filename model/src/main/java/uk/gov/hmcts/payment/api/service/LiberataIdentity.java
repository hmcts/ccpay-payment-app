package uk.gov.hmcts.payment.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.dto.liberata.identity.LiberataIdentityResponse;
import uk.gov.hmcts.payment.api.dto.liberata.identity.TokenResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import uk.gov.hmcts.payment.api.mapper.liberata.identity.AccessTokenDtoToTokenResponseMapper;
import uk.gov.hmcts.payment.api.v1.model.exceptions.LiberataIdentityException;


@Service
public class LiberataIdentity {

    private TokenResponse cachedToken;

    @Autowired()
    @Qualifier("liberataRestTemplate")
    private RestTemplate liberataRestTemplate;

    @Autowired()
    private AccessTokenDtoToTokenResponseMapper accessTokenDtoToTokenResponseMapper;


    @Value("${liberata.api.realtime.account.url}")
    private String baseUrl;

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
        final HttpHeaders headers = new HttpHeaders();
        final MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        formData.add("email", "PBA_UAT@liberata.com");
        formData.add("password", "GoNVHA>qSZh2(y\\,ABC;");
        final HttpEntity<MultiValueMap<String, String>> request =
            new HttpEntity<>(formData, headers);
        try
        {
            ResponseEntity<LiberataIdentityResponse> response =
                liberataRestTemplate.postForEntity(baseUrl, request, LiberataIdentityResponse.class);

            System.out.println("Error fetching token from Liberata: " + response);
            System.out.println("Error fetching token from Liberata: " + response.getBody());
            return accessTokenDtoToTokenResponseMapper.toTokenResponse(response.getBody());

        } catch (Exception exception) {
            System.out.println("Error fetching token from Liberata: " + exception.getMessage());
            exception.printStackTrace();
            throw new LiberataIdentityException("Error fetching token from Liberata: " + exception.getMessage(), exception);
        }
    }
}
