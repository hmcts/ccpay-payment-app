package uk.gov.hmcts.payment.api.v1.model.exceptions;

public class InvalidFeeRequestException extends PaymentException {

    public InvalidFeeRequestException() {}

    public InvalidFeeRequestException(String message) {
        super(message);
    }

}
