package uk.gov.hmcts.payment.api.exception;

public class PbaAccountStatusException extends RuntimeException {
    public PbaAccountStatusException(String message) {
        super(message);
    }
}
