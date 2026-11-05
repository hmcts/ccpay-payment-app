package uk.gov.hmcts.payment.api.service;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.exception.SystemUserTokenException;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.OAuth2Configuration;
import uk.gov.hmcts.reform.idam.client.models.TokenResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

/**
 * Gets an IDAM access token for payment-app's system user, using the OAuth2 password grant through
 * idam-java-client.
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

    // A token is replaced this long before it expires, so it does not expire while a request is using it.
    static final Duration EXPIRY_MARGIN = Duration.ofMinutes(1);

    private final IdamClient idamClient;
    private final OAuth2Configuration oauth2Configuration;
    private final String username;
    private final String password;
    private final Clock clock;

    private String accessToken;
    private Instant refreshAt;

    @Autowired
    public IdamSystemUserTokenProvider(IdamClient idamClient,
                                       OAuth2Configuration oauth2Configuration,
                                       @Value("${idam.system-user.username:}") String username,
                                       @Value("${idam.system-user.password:}") String password) {
        this(idamClient, oauth2Configuration, username, password, Clock.systemUTC());
    }

    IdamSystemUserTokenProvider(IdamClient idamClient, OAuth2Configuration oauth2Configuration, String username,
                                String password, Clock clock) {
        this.idamClient = idamClient;
        this.oauth2Configuration = oauth2Configuration;
        this.username = username;
        this.password = password;
        this.clock = clock;
    }

    /**
     * Whether the system user's credentials and the IDAM client have been configured.
     */
    public boolean isConfigured() {
        return Stream.of(username, password, oauth2Configuration.getClientId(), oauth2Configuration.getClientSecret())
            .noneMatch(IdamSystemUserTokenProvider::isBlank);
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
        TokenResponse response;
        try {
            log.info("Getting an IDAM token for the system user");
            response = idamClient.getAccessTokenResponse(username, password);
        } catch (FeignException e) {
            // The exception message can include the response body but never the request body, so the password is not logged.
            log.error("Unable to get an IDAM token for the system user", e);
            throw new SystemUserTokenException("unable to get an IDAM token for the system user", e);
        }
        if (response == null || isBlank(response.accessToken)) {
            throw new SystemUserTokenException("IDAM did not return an access token for the system user");
        }

        accessToken = response.accessToken;
        refreshAt = now.plus(lifetime(response.expiresIn)).minus(EXPIRY_MARGIN);
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
