package uk.gov.hmcts.payment.api.service;

import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
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
    private static final Logger LOG = LoggerFactory.getLogger(LiberataRealTimeAPI.class);

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
    private String liberataUsername;

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
        formData.add("email", liberataUsername);
        formData.add("password", liberataPassword);
        val request = new HttpEntity<MultiValueMap<String, String>>(formData, headers);

        try {
            val response = liberataRestTemplate.postForEntity(baseUrl + "/api/auth/token", request, LiberataIdentityResponse.class);
            return accessTokenDtoToTokenResponseMapper.toTokenResponse(response.getBody());
        } catch (Exception exception) {
            throw new LiberataIdentityException("Error fetching token from Liberata: " + exception.getMessage(), exception);
        }
    }

    public AccountDto getAccountDetails(String accessToken, String pbaCode) {
        try {
            return fetchAccountDetails(accessToken, pbaCode);
        } catch (HttpClientErrorException exception) {
            return handleAccountClientError(exception, pbaCode);
        } catch (ResourceAccessException exception) {
            LOG.error("Error connecting to Liberata for PBA code {}: {}", pbaCode, exception.getMessage(), exception);
            throw exception;
        } catch (Exception exception) {
            throw accountDetailsException(pbaCode, exception);
        }
    }

    private AccountDto handleAccountClientError(HttpClientErrorException exception, String pbaCode) {
        if (hasStatus(exception, HttpStatus.NOT_FOUND)) {
            LOG.warn("Account details not found in Liberata for PBA code {}: {}", pbaCode, exception.getMessage());
            throw exception;
        }
        if (hasStatus(exception, HttpStatus.UNAUTHORIZED)) {
            return retryWithRefreshedToken(pbaCode);
        }
        throw accountDetailsException(pbaCode, exception);
    }

    private AccountDto retryWithRefreshedToken(String pbaCode) {
        LOG.warn("Unauthorized fetching account details from Liberata for PBA code {}. Refreshing token and retrying once.", pbaCode);
        TokenResponse refreshedToken = refreshToken();
        try {
            return fetchAccountDetails(refreshedToken.getAccessToken(), pbaCode);
        } catch (HttpClientErrorException exception) {
            if (hasStatus(exception, HttpStatus.NOT_FOUND)) {
                LOG.warn("Account details not found in Liberata for PBA code {} after token refresh: {}", pbaCode, exception.getMessage());
                throw exception;
            }
            throw accountDetailsException(pbaCode, exception, true);
        } catch (ResourceAccessException exception) {
            LOG.error("Error connecting to Liberata for PBA code {} after token refresh: {}", pbaCode, exception.getMessage(), exception);
            throw exception;
        } catch (LiberataIdentityException exception) {
            throw exception;
        } catch (Exception exception) {
            throw accountDetailsException(pbaCode, exception, true);
        }
    }

    private boolean hasStatus(HttpClientErrorException exception, HttpStatus status) {
        return exception.getStatusCode().value() == status.value();
    }

    private LiberataIdentityException accountDetailsException(String pbaCode, Exception exception) {
        return accountDetailsException(pbaCode, exception, false);
    }

    private LiberataIdentityException accountDetailsException(String pbaCode, Exception exception, boolean afterTokenRefresh) {
        String refreshContext = afterTokenRefresh ? " after token refresh" : "";
        LOG.error("Error fetching account details from Liberata for PBA code {}{}: {}", pbaCode, refreshContext, exception.getMessage(), exception);
        return new LiberataIdentityException("Error fetching account details from Liberata: " + exception.getMessage(), exception);
    }

    private AccountDto fetchAccountDetails(String accessToken, String pbaCode) {
        val headers = new HttpHeaders();
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(accessToken);
        val request = new HttpEntity<Void>(headers);

        val response = liberataRestTemplate.exchange(
            baseUrl + "/api/account/" + pbaCode,
            HttpMethod.GET,
            request,
            LiberataAccountResponse.class
        );
        return liberataAccountMapper.toAccountDto(response.getBody());
    }
}
