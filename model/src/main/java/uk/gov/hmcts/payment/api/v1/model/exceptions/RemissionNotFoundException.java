package uk.gov.hmcts.payment.api.v1.model.exceptions;

public class RemissionNotFoundException extends PaymentException {

    public RemissionNotFoundException() {}

    public RemissionNotFoundException(String message) {
        super(message);
    }

}
