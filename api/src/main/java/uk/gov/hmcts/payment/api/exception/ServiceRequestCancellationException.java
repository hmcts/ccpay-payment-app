package uk.gov.hmcts.payment.api.exception;

public class ServiceRequestCancellationException extends RuntimeException {

    public ServiceRequestCancellationException(String message) {
        super(message);
    }
}
