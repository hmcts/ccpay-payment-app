package uk.gov.hmcts.payment.api.exception;

public class InvalidServiceRequestCancellationException extends RuntimeException {

    public InvalidServiceRequestCancellationException(String message) {
        super(message);
    }
}
