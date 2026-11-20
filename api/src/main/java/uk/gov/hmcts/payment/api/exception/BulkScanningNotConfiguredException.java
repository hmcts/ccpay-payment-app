package uk.gov.hmcts.payment.api.exception;

/**
 * Raised when a service request's cancellation needs the bulk-scanning check, but Payment App has no IDAM system user
 * configured to make it. This is a fault in Payment App's configuration, not in the request or the service request,
 * so it is reported as 503 rather than as an ineligible service request (409).
 */
public class BulkScanningNotConfiguredException extends RuntimeException {
    public BulkScanningNotConfiguredException(String message) {
        super(message);
    }
}
