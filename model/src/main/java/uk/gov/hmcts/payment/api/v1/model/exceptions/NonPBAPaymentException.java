package uk.gov.hmcts.payment.api.v1.model.exceptions;

public class NonPBAPaymentException extends RuntimeException {
    public NonPBAPaymentException(String message) {
        super(message);
    }
}
