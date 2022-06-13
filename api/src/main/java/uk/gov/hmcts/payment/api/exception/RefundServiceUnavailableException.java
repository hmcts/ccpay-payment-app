package uk.gov.hmcts.payment.api.exception;

public class RefundServiceUnavailableException  extends RuntimeException {
    public RefundServiceUnavailableException(String message) {
        super(message);
    }
}
