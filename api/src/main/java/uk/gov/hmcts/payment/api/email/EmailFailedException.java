package uk.gov.hmcts.payment.api.email;

public class EmailFailedException extends RuntimeException {
    public EmailFailedException(String message) {
        super(message);
    }
}
