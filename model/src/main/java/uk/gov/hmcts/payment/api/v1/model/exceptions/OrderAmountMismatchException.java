package uk.gov.hmcts.payment.api.v1.model.exceptions;

public class OrderAmountMismatchException extends PaymentException {

    public OrderAmountMismatchException() {}

    public OrderAmountMismatchException(String message) {
        super(message);
    }

}
