package uk.gov.hmcts.payment.api.exception;

public class BulkScanningServiceUnavailableException extends RuntimeException {
    public BulkScanningServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
