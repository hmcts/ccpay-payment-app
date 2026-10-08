package uk.gov.hmcts.payment.api.service;

import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.dto.liberata.account.LiberataAccountResponse;
import uk.gov.hmcts.payment.api.dto.liberata.identity.LiberataIdentityResponse;
import uk.gov.hmcts.payment.api.dto.liberata.identity.TokenResponse;
import uk.gov.hmcts.payment.api.mapper.liberata.account.LiberataAccountMapper;
import uk.gov.hmcts.payment.api.mapper.liberata.identity.AccessTokenDtoToTokenResponseMapper;
import uk.gov.hmcts.payment.api.v1.model.exceptions.LiberataIdentityException;


@Service
public class LiberataRealTimeAPI {

    private TokenResponse cachedToken;

    @Autowired()
    @Qualifier("liberataRestTemplate")
    private RestTemplate liberataRestTemplate;

    @Autowired()
    private AccessTokenDtoToTokenResponseMapper accessTokenDtoToTokenResponseMapper;

    @Autowired
    private LiberataAccountMapper liberataAccountMapper;

    @Value("${liberata.api.realtime.account.url}")
    private String baseUrl;

    @Value("${liberata.api.realtime.account.username}")
    private String lieberataUsername;

    @Value("${liberata.api.realtime.account.password}")
    private String liberataPassword;

    private TokenResponse getToken() {
        if (cachedToken != null && !cachedToken.isExpired()) {
            return cachedToken;
        }
        cachedToken = fetchNewToken();
        return cachedToken;
    }

    @Cacheable(value = "liberataToken", sync = true)
    public TokenResponse getValidToken() {
        val  token = getToken();
        return token.isExpired() ? refreshToken() : token;
    }

    @CachePut(value = "liberataToken")
    public TokenResponse refreshToken() {
        return fetchNewToken();
    }

    private TokenResponse fetchNewToken() {
        val headers = new HttpHeaders();
        val formData = new LinkedMultiValueMap<String, String>();

        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        formData.add("email", lieberataUsername);
        formData.add("password", liberataPassword);
        val request = new HttpEntity<MultiValueMap<String, String>>(formData, headers);

        try {
            val response = liberataRestTemplate.postForEntity(baseUrl + "/pba-api-v2-uat/api/auth/token", request, LiberataIdentityResponse.class);
            return accessTokenDtoToTokenResponseMapper.toTokenResponse(response.getBody());
        } catch (Exception exception) {
            throw new LiberataIdentityException("Error fetching token from Liberata: " + exception.getMessage(), exception);
        }
    }

    public AccountDto getAccountDetails(String accessToken, String pbaCode) {

        val headers = new HttpHeaders();
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(accessToken);
        val request = new HttpEntity<Void>(headers);

        try {
            // As above, baseUrl should really include the environment-specific path (e.g.pba-api-v2-uat)??
            val response = liberataRestTemplate.exchange(
                baseUrl + "/pba-api-v2-uat/api/account/" + pbaCode,
                HttpMethod.GET,
                request,
                LiberataAccountResponse.class
            );
            return liberataAccountMapper.toAccountDto(response.getBody());
        } catch (Exception exception) {
            throw new LiberataIdentityException("Error fetching account details from Liberata: " + exception.getMessage(), exception);
        }
    }
}
