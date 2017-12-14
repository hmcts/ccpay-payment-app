package uk.gov.hmcts.payment.api.model.exceptions;

public class PaymentRefDataNotFoundException extends PaymentException {

    public PaymentRefDataNotFoundException(String message) {
        super(message);
    }
}
