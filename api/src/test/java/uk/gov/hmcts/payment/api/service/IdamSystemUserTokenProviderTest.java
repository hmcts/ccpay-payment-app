package uk.gov.hmcts.payment.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.exception.SystemUserTokenException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Checks the system user's token is requested from IDAM with the password grant, cached until shortly before it
 * expires, and that a missing configuration or a failed request is reported as a {@link SystemUserTokenException}.
 */
class IdamSystemUserTokenProviderTest {

    private static final String IDAM_URL = "http://idam-api";
    private static final String TOKEN_URL = IDAM_URL + "/o/token";

    private MockRestServiceServer server;
    private RestTemplate restTemplate;
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        clock = new MutableClock(Instant.parse("2026-10-30T09:00:00Z"));
    }

    @Test
    void requestsATokenWithThePasswordGrant() {
        MultiValueMap<String, String> expectedForm = new LinkedMultiValueMap<>();
        expectedForm.add("grant_type", "password");
        expectedForm.add("username", "system.user@hmcts.net");
        expectedForm.add("password", "secret-password");
        expectedForm.add("client_id", "payment_api");
        expectedForm.add("client_secret", "client-secret");
        expectedForm.add("scope", "openid profile roles");
        expectedForm.add("redirect_uri", "http://payment-api/oauth2/callback");

        server.expect(requestTo(TOKEN_URL))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(content().formData(expectedForm))
            .andRespond(tokenResponse("token-1", "28800"));

        assertThat(provider("http://payment-api/oauth2/callback").getAccessToken()).isEqualTo("token-1");
        server.verify();
    }

    @Test
    void leavesOutTheRedirectUriWhenItIsNotSet() {
        server.expect(requestTo(TOKEN_URL))
            .andExpect(request -> assertThat(((MockClientHttpRequest) request).getBodyAsString()).doesNotContain("redirect_uri"))
            .andRespond(tokenResponse("token-1", "28800"));

        assertThat(provider("").getAccessToken()).isEqualTo("token-1");
        server.verify();
    }

    @Test
    void reusesTheTokenUntilShortlyBeforeItExpires() {
        IdamSystemUserTokenProvider provider = provider("");
        server.expect(ExpectedCount.once(), requestTo(TOKEN_URL)).andRespond(tokenResponse("token-1", "3600"));

        assertThat(provider.getAccessToken()).isEqualTo("token-1");
        clock.advance(Duration.ofSeconds(3600).minus(IdamSystemUserTokenProvider.EXPIRY_MARGIN).minusSeconds(1));
        assertThat(provider.getAccessToken()).isEqualTo("token-1");
        server.verify();

        server.reset();
        server.expect(ExpectedCount.once(), requestTo(TOKEN_URL)).andRespond(tokenResponse("token-2", "3600"));
        clock.advance(Duration.ofSeconds(1));
        assertThat(provider.getAccessToken()).isEqualTo("token-2");
        server.verify();
    }

    @Test
    void doesNotCacheATokenWithoutAUsableExpiry() {
        IdamSystemUserTokenProvider provider = provider("");
        server.expect(ExpectedCount.twice(), requestTo(TOKEN_URL)).andRespond(tokenResponse("token-1", "\"soon\""));

        provider.getAccessToken();
        provider.getAccessToken();
        server.verify();
    }

    @Test
    void reportsAnIdamFailure() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> provider("").getAccessToken())
            .isInstanceOf(SystemUserTokenException.class)
            .hasMessage("unable to get an IDAM token for the system user");
    }

    @Test
    void reportsAResponseWithoutAnAccessToken() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess("{\"expires_in\": 28800}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider("").getAccessToken())
            .isInstanceOf(SystemUserTokenException.class)
            .hasMessage("IDAM did not return an access token for the system user");
    }

    @Test
    void isNotConfiguredWithoutTheUserOrClientCredentials() {
        assertThat(provider("").isConfigured()).isTrue();
        assertThat(new IdamSystemUserTokenProvider(restTemplate, IDAM_URL, "", "secret-password", "payment_api",
            "client-secret", "openid profile roles", "", clock).isConfigured()).isFalse();
        assertThat(new IdamSystemUserTokenProvider(restTemplate, IDAM_URL, "system.user@hmcts.net", "secret-password",
            "payment_api", " ", "openid profile roles", "", clock).isConfigured()).isFalse();
    }

    @Test
    void refusesToRequestATokenWhenNotConfigured() {
        IdamSystemUserTokenProvider provider = new IdamSystemUserTokenProvider(restTemplate, IDAM_URL, "", "", "", "",
            "openid profile roles", "", clock);

        assertThatThrownBy(provider::getAccessToken)
            .isInstanceOf(SystemUserTokenException.class)
            .hasMessage("the IDAM system user is not configured");
        server.verify();
    }

    private IdamSystemUserTokenProvider provider(String redirectUri) {
        return new IdamSystemUserTokenProvider(restTemplate, IDAM_URL, "system.user@hmcts.net", "secret-password",
            "payment_api", "client-secret", "openid profile roles", redirectUri, clock);
    }

    private static ResponseCreator tokenResponse(String accessToken, String expiresIn) {
        return withSuccess("""
            {"access_token": "%s", "token_type": "Bearer", "scope": "openid profile roles", "expires_in": %s}
            """.formatted(accessToken, expiresIn), MediaType.APPLICATION_JSON);
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
