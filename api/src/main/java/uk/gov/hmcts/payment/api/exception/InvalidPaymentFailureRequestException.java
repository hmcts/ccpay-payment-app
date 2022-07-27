package uk.gov.hmcts.payment.api.exception;

public class InvalidPaymentFailureRequestException extends RuntimeException {
    public InvalidPaymentFailureRequestException (String message) {
        super(message);
    }
}
