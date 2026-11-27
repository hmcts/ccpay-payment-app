package uk.gov.hmcts.payment.api.exception;

public class BulkScanningNotConfiguredException extends RuntimeException {
    public BulkScanningNotConfiguredException(String message) {
        super(message);
    }
}
