package uk.gov.hmcts.payment.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.dto.PaymentStatus;
import uk.gov.hmcts.payment.api.dto.SearchResponse;
import uk.gov.hmcts.payment.api.exception.BulkScanningServiceUnavailableException;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.net.SocketTimeoutException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Checks the bulk-scanning case lookup sends the user and service tokens to {@code GET /cases/{ccd_reference}}, reads the
 * response, treats 404 as no payments, and turns any other failure into a
 * {@link BulkScanningServiceUnavailableException}.
 */
class BulkScanningClientTest {

    private static final String BULK_SCANNING_URL = "http://bulk-scanning";
    private static final String CASE_URL = BULK_SCANNING_URL + "/cases/1234567890123456";
    private static final String CCD_CASE_NUMBER = "1234567890123456";
    private static final String USER_TOKEN = "Bearer user-token";

    private MockRestServiceServer server;
    private BulkScanningClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();

        AuthTokenGenerator authTokenGenerator = mock(AuthTokenGenerator.class);
        when(authTokenGenerator.generate()).thenReturn("s2s-token");

        client = new BulkScanningClient(restTemplate, authTokenGenerator, BULK_SCANNING_URL);
    }

    @Test
    void returnsTheSearchResponseForTheCase() {
        server.expect(requestTo(CASE_URL))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("ServiceAuthorization", "s2s-token"))
            .andExpect(header("Authorization", USER_TOKEN))
            .andRespond(withSuccess("""
                {"ccd_reference": "1234567890123456", "all_payments_status": "COMPLETE",
                 "payments": [{"dcn_reference": "DCN1", "amount": 100}]}
                """, MediaType.APPLICATION_JSON));

        Optional<SearchResponse> response = client.findPaymentsForCase(CCD_CASE_NUMBER, USER_TOKEN);

        assertThat(response).isPresent();
        assertThat(response.get().getAllPaymentsStatus()).isEqualTo(PaymentStatus.COMPLETE);
        assertThat(response.get().getPayments()).hasSize(1);
        server.verify();
    }

    @Test
    void returnsEmptyWhenBulkScanningHasNoPaymentsForTheCase() {
        server.expect(requestTo(CASE_URL))
            .andRespond(withStatus(HttpStatus.NOT_FOUND).body("Payments Not found for ccdReference"));

        assertThat(client.findPaymentsForCase(CCD_CASE_NUMBER, USER_TOKEN)).isEmpty();
    }

    @Test
    void reportsBulkScanningErrorsAsUnavailable() {
        server.expect(requestTo(CASE_URL)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.findPaymentsForCase(CCD_CASE_NUMBER, USER_TOKEN))
            .isInstanceOf(BulkScanningServiceUnavailableException.class);
    }

    @Test
    void reportsRejectedRequestsAsUnavailable() {
        server.expect(requestTo(CASE_URL)).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> client.findPaymentsForCase(CCD_CASE_NUMBER, USER_TOKEN))
            .isInstanceOf(BulkScanningServiceUnavailableException.class);
    }

    @Test
    void reportsTimeoutsAsUnavailable() {
        server.expect(requestTo(CASE_URL)).andRespond(withException(new SocketTimeoutException("read timed out")));

        assertThatThrownBy(() -> client.findPaymentsForCase(CCD_CASE_NUMBER, USER_TOKEN))
            .isInstanceOf(BulkScanningServiceUnavailableException.class);
    }
}
