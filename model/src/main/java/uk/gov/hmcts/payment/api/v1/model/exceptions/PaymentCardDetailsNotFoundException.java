package uk.gov.hmcts.payment.api.v1.model.exceptions;

public class PaymentCardDetailsNotFoundException extends PaymentException {

    public PaymentCardDetailsNotFoundException(String message) {
        super(message);
    }
}
