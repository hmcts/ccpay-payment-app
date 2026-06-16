package uk.gov.hmcts.payment.api.service;

import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.dto.liberata.identity.TokenResponse;

@Service
public class LiberataIdentity {

    private TokenResponse cachedToken;

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
        // call API
        return new TokenResponse();
    }


}
