package uk.gov.hmcts.payment.api.v1.model.exceptions;

public class LiberataAccountException extends RuntimeException {
    public LiberataAccountException(String message) {
        super(message);
    }
}
