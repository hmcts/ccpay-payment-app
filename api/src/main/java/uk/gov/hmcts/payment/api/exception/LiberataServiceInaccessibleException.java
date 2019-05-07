package uk.gov.hmcts.payment.api.exception;

public class LiberataServiceInaccessibleException extends RuntimeException {
    public LiberataServiceInaccessibleException(String message) {
        super(message);
    }
}
