package uk.gov.hmcts.payment.api.v1.model.exceptions;

public class PaymentRefDataNotFoundException extends PaymentException {

    public PaymentRefDataNotFoundException(String message) {
        super(message);
    }
}
