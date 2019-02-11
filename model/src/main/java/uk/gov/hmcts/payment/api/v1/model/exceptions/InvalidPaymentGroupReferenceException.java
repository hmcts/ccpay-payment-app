package uk.gov.hmcts.payment.api.v1.model.exceptions;

public class InvalidPaymentGroupReferenceException extends PaymentException {

    public InvalidPaymentGroupReferenceException() {}

    public InvalidPaymentGroupReferenceException(String message) {
        super(message);
    }

}
