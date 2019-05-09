package uk.gov.hmcts.payment.api.v1.model.exceptions;

public class DuplicatePaymentException extends RuntimeException {

    public DuplicatePaymentException(String message) {
        super(message);
    }
}
