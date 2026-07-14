package uk.gov.hmcts.payment.api.exceptions;

public class LiberataServiceException extends RuntimeException {
    public LiberataServiceException(String message) {
        super(message);
    }
}
