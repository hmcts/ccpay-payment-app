package uk.gov.hmcts.payment.api.service;

import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.payment.api.exception.SystemUserTokenException;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.OAuth2Configuration;
import uk.gov.hmcts.reform.idam.client.models.TokenResponse;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Checks the system user's token is cached until shortly before it expires, and that a missing configuration or a
 * failed request is reported as a {@link SystemUserTokenException}. The request itself is made by idam-java-client;
 * {@link BulkScanningSystemUserComponentTest} checks what it sends.
 */
class IdamSystemUserTokenProviderTest {

    private static final String USERNAME = "system.user@hmcts.net";
    private static final String PASSWORD = "secret-password";
    private static final OAuth2Configuration CLIENT =
        new OAuth2Configuration("", "payment_api", "client-secret", "openid profile roles");

    private IdamClient idamClient;
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        idamClient = mock(IdamClient.class);
        clock = new MutableClock(Instant.parse("2026-10-30T09:00:00Z"));
    }

    @Test
    void requestsATokenForTheSystemUser() {
        when(idamClient.getAccessTokenResponse(USERNAME, PASSWORD)).thenReturn(tokenResponse("token-1", "28800"));

        assertThat(provider().getAccessToken()).isEqualTo("token-1");
    }

    @Test
    void reusesTheTokenUntilShortlyBeforeItExpires() {
        IdamSystemUserTokenProvider provider = provider();
        when(idamClient.getAccessTokenResponse(USERNAME, PASSWORD))
            .thenReturn(tokenResponse("token-1", "3600"), tokenResponse("token-2", "3600"));

        assertThat(provider.getAccessToken()).isEqualTo("token-1");
        clock.advance(Duration.ofSeconds(3600).minus(IdamSystemUserTokenProvider.EXPIRY_MARGIN).minusSeconds(1));
        assertThat(provider.getAccessToken()).isEqualTo("token-1");
        clock.advance(Duration.ofSeconds(1));
        assertThat(provider.getAccessToken()).isEqualTo("token-2");
        verify(idamClient, times(2)).getAccessTokenResponse(USERNAME, PASSWORD);
    }

    @Test
    void doesNotCacheATokenWithoutAUsableExpiry() {
        IdamSystemUserTokenProvider provider = provider();
        when(idamClient.getAccessTokenResponse(USERNAME, PASSWORD)).thenReturn(tokenResponse("token-1", "soon"));

        provider.getAccessToken();
        provider.getAccessToken();
        verify(idamClient, times(2)).getAccessTokenResponse(USERNAME, PASSWORD);
    }

    @Test
    void reportsAnIdamFailure() {
        FeignException unauthorized = FeignException.errorStatus("IdamApi#generateOpenIdToken", feign.Response.builder()
            .status(401)
            .request(Request.create(Request.HttpMethod.POST, "http://idam-api/o/token", Map.of(), null,
                StandardCharsets.UTF_8, null))
            .build());
        when(idamClient.getAccessTokenResponse(any(), any())).thenThrow(unauthorized);

        assertThatThrownBy(() -> provider().getAccessToken())
            .isInstanceOf(SystemUserTokenException.class)
            .hasMessage("unable to get an IDAM token for the system user")
            .hasCause(unauthorized);
    }

    @Test
    void reportsAResponseWithoutAnAccessToken() {
        when(idamClient.getAccessTokenResponse(USERNAME, PASSWORD)).thenReturn(tokenResponse(null, "28800"));

        assertThatThrownBy(() -> provider().getAccessToken())
            .isInstanceOf(SystemUserTokenException.class)
            .hasMessage("IDAM did not return an access token for the system user");
    }

    @Test
    void isNotConfiguredWithoutTheUserOrClientCredentials() {
        assertThat(provider().isConfigured()).isTrue();
        assertThat(new IdamSystemUserTokenProvider(idamClient, CLIENT, "", PASSWORD, clock).isConfigured()).isFalse();
        assertThat(new IdamSystemUserTokenProvider(idamClient,
            new OAuth2Configuration("", "payment_api", " ", "openid profile roles"), USERNAME, PASSWORD, clock)
            .isConfigured()).isFalse();
    }

    @Test
    void refusesToRequestATokenWhenNotConfigured() {
        IdamSystemUserTokenProvider provider = new IdamSystemUserTokenProvider(idamClient,
            new OAuth2Configuration("", "", "", "openid profile roles"), "", "", clock);

        assertThatThrownBy(provider::getAccessToken)
            .isInstanceOf(SystemUserTokenException.class)
            .hasMessage("the IDAM system user is not configured");
        verifyNoInteractions(idamClient);
    }

    private IdamSystemUserTokenProvider provider() {
        return new IdamSystemUserTokenProvider(idamClient, CLIENT, USERNAME, PASSWORD, clock);
    }

    private static TokenResponse tokenResponse(String accessToken, String expiresIn) {
        return new TokenResponse(accessToken, expiresIn, null, null, "openid profile roles", "Bearer");
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
