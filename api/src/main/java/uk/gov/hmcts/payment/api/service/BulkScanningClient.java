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
    private final String bulkScanningUrl;

    public BulkScanningClient(@Qualifier("restTemplateBulkScanning") RestTemplate restTemplate,
                              AuthTokenGenerator authTokenGenerator,
                              @Value("${bulk.scanning.payments.processed.url}") String bulkScanningUrl) {
        this.restTemplate = restTemplate;
        this.authTokenGenerator = authTokenGenerator;
        this.bulkScanningUrl = bulkScanningUrl;
    }

    /**
     * Find the bulk-scanned payments held against a case, using {@code GET /cases/{ccd_reference}}.
     *
     * <p>Bulk scanning only lists the payments that are still waiting to be allocated. It returns
     * {@code all_payments_status} of {@code PROCESSED} when everything has been allocated, and an empty
     * response when it has no envelopes for the case.</p>
     *
     * @param ccdCaseNumber the CCD case number of the service request
     * @return the search response, or empty when bulk scanning reports no payments for the case (HTTP 404)
     * @throws BulkScanningServiceUnavailableException when bulk scanning cannot be reached or returns an error
     */
    public Optional<SearchResponse> findPaymentsForCase(String ccdCaseNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("ServiceAuthorization", authTokenGenerator.generate());
        // TODO PAY-9025: bulk scanning secures GET /cases/** with a user token holding the payments or citizen role,
        // and requires an Authorization header. An automated cancellation has no user token, so bulk scanning
        // must allow service-only access here (as it does for GET /case/**) or this app needs a system user.

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
