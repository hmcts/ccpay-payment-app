package uk.gov.hmcts.payment.api.exceptions;

public class PaymentServiceNotFoundException extends RuntimeException {
    public PaymentServiceNotFoundException(String message) {
        super(message);
    }
}
