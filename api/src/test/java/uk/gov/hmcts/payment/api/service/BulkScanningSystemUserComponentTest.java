package uk.gov.hmcts.payment.api.service;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.payment.api.exception.BulkScanningServiceUnavailableException;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Checks the bulk scanning call at the HTTP boundary, with WireMock standing in for both IDAM and bulk scanning.
 *
 * <p>Only the properties the chart sets are configured here, so this also checks that they reach the IDAM token
 * request. The system user's token is cached for the life of the context, so the timeout test runs first, while
 * nothing is cached.</p>
 */
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BulkScanningSystemUserComponentTest {

    private static final String CCD_CASE_NUMBER = "1234567890123456";
    private static final String CASE_PATH = "/cases/" + CCD_CASE_NUMBER;
    private static final String TOKEN_PATH = "/o/token";
    private static final int READ_TIMEOUT_MILLIS = 1000;

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("auth.idam.client.baseUrl", wireMock::baseUrl);
        registry.add("bulk.scanning.payments.processed.url", wireMock::baseUrl);
        registry.add("idam.system-user.username", () -> "system.user@hmcts.net");
        registry.add("idam.system-user.password", () -> "secret-password");
        registry.add("idam.client.id", () -> "payment_api");
        registry.add("idam.client.secret", () -> "client-secret");
        registry.add("idam.client.redirect_uri", () -> "http://payment-api/oauth2/callback");
        registry.add("spring.cloud.openfeign.client.config.idam-api.read-timeout", () -> READ_TIMEOUT_MILLIS);
    }

    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;

    @Autowired
    private BulkScanningClient bulkScanningClient;

    @BeforeEach
    void setUp() {
        when(authTokenGenerator.generate()).thenReturn("Bearer s2s-token");
        wireMock.stubFor(get(urlEqualTo(CASE_PATH))
            .willReturn(okJson("{\"all_payments_status\": \"PROCESSED\"}")));
    }

    @Test
    @Order(1)
    void reportsASlowIdamAsUnavailableWithoutCallingBulkScanning() {
        wireMock.stubFor(post(urlEqualTo(TOKEN_PATH))
            .willReturn(tokenResponse("token-slow").withFixedDelay(READ_TIMEOUT_MILLIS * 3)));

        long start = System.nanoTime();
        assertThatThrownBy(() -> bulkScanningClient.findPaymentsForCase(CCD_CASE_NUMBER))
            .isInstanceOf(BulkScanningServiceUnavailableException.class);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMillis).isLessThan(READ_TIMEOUT_MILLIS * 3L);
        wireMock.verify(0, getRequestedFor(urlEqualTo(CASE_PATH)));
    }

    @Test
    @Order(2)
    void callsBulkScanningWithTheSystemUserTokenAndReusesIt() {
        wireMock.stubFor(post(urlEqualTo(TOKEN_PATH)).willReturn(tokenResponse("token-1")));

        assertThat(bulkScanningClient.findPaymentsForCase(CCD_CASE_NUMBER))
            .hasValueSatisfying(response -> assertThat(response.getAllPaymentsStatus()).hasToString("PROCESSED"));
        assertThat(bulkScanningClient.findPaymentsForCase(CCD_CASE_NUMBER)).isPresent();

        wireMock.verify(1, postRequestedFor(urlEqualTo(TOKEN_PATH))
            .withHeader("Content-Type", containing(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
            .withFormParam("grant_type", equalTo("password"))
            .withFormParam("username", equalTo("system.user@hmcts.net"))
            .withFormParam("password", equalTo("secret-password"))
            .withFormParam("client_id", equalTo("payment_api"))
            .withFormParam("client_secret", equalTo("client-secret"))
            .withFormParam("redirect_uri", equalTo("http://payment-api/oauth2/callback"))
            .withFormParam("scope", equalTo("openid profile roles")));
        wireMock.verify(2, getRequestedFor(urlEqualTo(CASE_PATH))
            .withHeader("Authorization", equalTo("Bearer token-1"))
            .withHeader("ServiceAuthorization", equalTo("Bearer s2s-token")));
    }

    private static ResponseDefinitionBuilder tokenResponse(String accessToken) {
        return okJson("""
            {"access_token": "%s", "token_type": "Bearer", "expires_in": 28800}
            """.formatted(accessToken));
    }
}
