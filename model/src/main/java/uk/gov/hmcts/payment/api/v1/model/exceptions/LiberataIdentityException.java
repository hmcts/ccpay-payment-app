package uk.gov.hmcts.payment.api.v1.model.exceptions;

public class LiberataIdentityException extends RuntimeException {
    public LiberataIdentityException(String message) {
        super(message);
    }

    public LiberataIdentityException(String message, Exception cause) {
        super(message, cause);
    }
}
