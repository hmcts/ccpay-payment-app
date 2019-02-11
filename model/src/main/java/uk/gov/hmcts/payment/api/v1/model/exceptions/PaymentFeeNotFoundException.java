package uk.gov.hmcts.payment.api.v1.model.exceptions;

public class PaymentFeeNotFoundException extends PaymentException {

    public PaymentFeeNotFoundException() {}

    public PaymentFeeNotFoundException(String message) {
        super(message);
    }

}
