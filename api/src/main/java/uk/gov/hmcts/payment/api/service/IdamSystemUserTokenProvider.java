package uk.gov.hmcts.payment.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.dto.idam.IdamTokenResponse;
import uk.gov.hmcts.payment.api.exception.SystemUserTokenException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

/**
 * Gets an IDAM access token for payment-app's system user, using the OAuth2 password grant.
 *
 * <p>Some services only answer requests made with a user token, even for lookups that payment-app makes on its own
 * behalf. The bulk scanning case search during service request cancellation is one of these.</p>
 *
 * <p>The token is cached until shortly before it expires. Callers may be holding a database lock while they wait,
 * so fetching a new token for every call would hold that lock for longer than needed.</p>
 */
@Slf4j
@Service
public class IdamSystemUserTokenProvider {

    static final String TOKEN_ENDPOINT = "/o/token";

    // A token is replaced this long before it expires, so it does not expire while a request is using it.
    static final Duration EXPIRY_MARGIN = Duration.ofMinutes(1);

    private final RestTemplate restTemplate;
    private final String idamBaseUrl;
    private final String username;
    private final String password;
    private final String clientId;
    private final String clientSecret;
    private final String scope;
    private final String redirectUri;
    private final Clock clock;

    private String accessToken;
    private Instant refreshAt;

    @Autowired
    public IdamSystemUserTokenProvider(@Qualifier("restTemplateIdamSystemUser") RestTemplate restTemplate,
                                       @Value("${auth.idam.client.baseUrl}") String idamBaseUrl,
                                       @Value("${idam.system-user.username:}") String username,
                                       @Value("${idam.system-user.password:}") String password,
                                       @Value("${idam.system-user.client-id:}") String clientId,
                                       @Value("${idam.system-user.client-secret:}") String clientSecret,
                                       @Value("${idam.system-user.scope:openid profile roles}") String scope,
                                       @Value("${idam.system-user.redirect-uri:}") String redirectUri) {
        this(restTemplate, idamBaseUrl, username, password, clientId, clientSecret, scope, redirectUri,
            Clock.systemUTC());
    }

    IdamSystemUserTokenProvider(RestTemplate restTemplate, String idamBaseUrl, String username, String password,
                                String clientId, String clientSecret, String scope, String redirectUri, Clock clock) {
        this.restTemplate = restTemplate;
        this.idamBaseUrl = idamBaseUrl;
        this.username = username;
        this.password = password;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
        this.redirectUri = redirectUri;
        this.clock = clock;
    }

    /**
     * Whether the system user's credentials and the IDAM client have been configured.
     */
    public boolean isConfigured() {
        return Stream.of(username, password, clientId, clientSecret).noneMatch(IdamSystemUserTokenProvider::isBlank);
    }

    /**
     * Get an access token for the system user, fetching a new one from IDAM if there is no cached token or it is
     * about to expire.
     *
     * @return the access token, without the {@code Bearer} prefix
     * @throws SystemUserTokenException when the system user is not configured or IDAM does not return a token
     */
    public synchronized String getAccessToken() {
        if (!isConfigured()) {
            throw new SystemUserTokenException("the IDAM system user is not configured");
        }
        Instant now = clock.instant();
        if (accessToken == null || !now.isBefore(refreshAt)) {
            fetchAccessToken(now);
        }
        return accessToken;
    }

    private void fetchAccessToken(Instant now) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("username", username);
        form.add("password", password);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("scope", scope);
        if (!isBlank(redirectUri)) {
            form.add("redirect_uri", redirectUri);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        IdamTokenResponse response;
        try {
            log.info("Getting an IDAM token for the system user");
            response = restTemplate.postForObject(idamBaseUrl + TOKEN_ENDPOINT, new HttpEntity<>(form, headers),
                IdamTokenResponse.class);
        } catch (RestClientException e) {
            // The exception message can include the response body but never the request, so the password is not logged.
            log.error("Unable to get an IDAM token for the system user", e);
            throw new SystemUserTokenException("unable to get an IDAM token for the system user", e);
        }
        if (response == null || isBlank(response.getAccessToken())) {
            throw new SystemUserTokenException("IDAM did not return an access token for the system user");
        }

        accessToken = response.getAccessToken();
        refreshAt = now.plus(lifetime(response.getExpiresIn())).minus(EXPIRY_MARGIN);
    }

    // When IDAM does not say how long the token lasts, it is used once and not cached.
    private static Duration lifetime(String expiresIn) {
        try {
            return Duration.ofSeconds(Long.parseLong(expiresIn));
        } catch (NumberFormatException e) {
            log.warn("IDAM returned an unexpected expires_in of {}, so the system user token will not be cached", expiresIn);
            return Duration.ZERO;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
