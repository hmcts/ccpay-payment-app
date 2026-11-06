package uk.gov.hmcts.payment.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.dto.SearchResponse;
import uk.gov.hmcts.payment.api.exception.BulkScanningServiceUnavailableException;
import uk.gov.hmcts.payment.api.exception.SystemUserTokenException;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.util.Optional;

/**
 * Read-only client for the bulk-scanning app (ccpay-bulkscanning-app).
 */
@Slf4j
@Service
public class BulkScanningClient {

    private final RestTemplate restTemplate;
    private final AuthTokenGenerator authTokenGenerator;
    private final IdamSystemUserTokenProvider systemUserTokenProvider;
    private final String bulkScanningUrl;

    public BulkScanningClient(@Qualifier("restTemplateBulkScanning") RestTemplate restTemplate,
                              AuthTokenGenerator authTokenGenerator,
                              IdamSystemUserTokenProvider systemUserTokenProvider,
                              @Value("${bulk.scanning.payments.processed.url}") String bulkScanningUrl) {
        this.restTemplate = restTemplate;
        this.authTokenGenerator = authTokenGenerator;
        this.systemUserTokenProvider = systemUserTokenProvider;
        this.bulkScanningUrl = bulkScanningUrl;
    }

    /**
     * Whether this client can authenticate to bulk scanning, which needs the IDAM system user to be configured.
     */
    public boolean canAuthenticate() {
        return systemUserTokenProvider.isConfigured();
    }

    /**
     * Find the bulk-scanned payments held against a case, using {@code GET /cases/{ccd_reference}}.
     *
     * <p>Bulk scanning only lists the payments that are still waiting to be allocated. It returns
     * {@code all_payments_status} of {@code PROCESSED} when everything has been allocated, and an empty
     * response when it has no envelopes for the case.</p>
     *
     * <p>Bulk scanning secures this endpoint with a user token holding the {@code payments} or {@code citizen}
     * role, as well as a service token. The system user's token is sent for every cancellation, manual or
     * automated, as bulk scanning only checks the role and not who the user is.</p>
     *
     * @param ccdCaseNumber the CCD case number of the service request
     * @return the search response, or empty when bulk scanning reports no payments for the case (HTTP 404)
     * @throws BulkScanningServiceUnavailableException when no system user token can be obtained, or bulk scanning
     *                                                 cannot be reached or returns an error
     */
    public Optional<SearchResponse> findPaymentsForCase(String ccdCaseNumber) {
        HttpHeaders headers = new HttpHeaders();
        try {
            headers.setBearerAuth(systemUserTokenProvider.getAccessToken());
        } catch (SystemUserTokenException e) {
            throw new BulkScanningServiceUnavailableException("unable to check bulk scanning for unallocated payments", e);
        }
        headers.set("ServiceAuthorization", authTokenGenerator.generate());

        try {
            log.info("Calling bulk scanning for payments against case {}", ccdCaseNumber);
            return Optional.ofNullable(restTemplate.exchange(
                "%s/cases/{ccd_reference}".formatted(bulkScanningUrl),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                SearchResponse.class,
                ccdCaseNumber).getBody());
        } catch (HttpClientErrorException.NotFound e) {
            log.info("Bulk scanning has no payments for case {}", ccdCaseNumber);
            return Optional.empty();
        } catch (RestClientException e) {
            log.error("Bulk scanning lookup failed for case {}", ccdCaseNumber, e);
            throw new BulkScanningServiceUnavailableException("unable to check bulk scanning for unallocated payments", e);
        }
    }
}
