package uk.gov.hmcts.payment.api.v1.model.exceptions;

public class PaymentGroupNotFoundException extends PaymentException {

    public PaymentGroupNotFoundException() {}

    public PaymentGroupNotFoundException(String message) {
        super(message);
    }

}
