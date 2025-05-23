package uk.gov.hmcts.payment.api.v1.model.exceptions;

public class TelephonyServiceException extends RuntimeException {
    public TelephonyServiceException(String message) {
        super(message);
    }
}
