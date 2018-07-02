package uk.gov.hmcts.payment.api.v1.model.exceptions;

public class PaymentNotFoundException extends PaymentException {

    public PaymentNotFoundException() {}

    public PaymentNotFoundException(String message) {
        super(message);
    }

}
