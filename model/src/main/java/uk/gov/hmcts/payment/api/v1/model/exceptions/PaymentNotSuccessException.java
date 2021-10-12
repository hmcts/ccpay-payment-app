package uk.gov.hmcts.payment.api.v1.model.exceptions;

public class PaymentNotSuccessException extends RuntimeException {
    public PaymentNotSuccessException(String message) {
        super(message);
    }
}
