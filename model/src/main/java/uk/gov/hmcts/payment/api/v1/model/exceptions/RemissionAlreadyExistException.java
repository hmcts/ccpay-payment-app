package uk.gov.hmcts.payment.api.v1.model.exceptions;

public class RemissionAlreadyExistException extends PaymentException {

    public RemissionAlreadyExistException() {}

    public RemissionAlreadyExistException(String message) {
        super(message);
    }

}
