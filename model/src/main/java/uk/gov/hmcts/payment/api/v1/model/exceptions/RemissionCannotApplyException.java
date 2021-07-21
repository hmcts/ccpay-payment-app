package uk.gov.hmcts.payment.api.v1.model.exceptions;

public class RemissionCannotApplyException extends PaymentException {

    public RemissionCannotApplyException() {}

    public RemissionCannotApplyException(String message) {
        super(message);
    }

}
