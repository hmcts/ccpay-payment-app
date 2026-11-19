package uk.gov.hmcts.payment.api.exception;

public class IACServiceException extends RuntimeException {
    public IACServiceException(String message) {
        super(message);
    }
    public IACServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
