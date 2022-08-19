package uk.gov.hmcts.payment.api.exception;

public class FailureReferenceNotFoundException extends RuntimeException {
    public FailureReferenceNotFoundException (String message) {
        super(message);
    }
}
