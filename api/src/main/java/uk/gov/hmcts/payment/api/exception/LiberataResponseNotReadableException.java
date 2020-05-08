package uk.gov.hmcts.payment.api.exception;

public class LiberataResponseNotReadableException extends RuntimeException {
    public LiberataResponseNotReadableException(String message) {
        super(message);
    }
}
