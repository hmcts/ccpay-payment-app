package uk.gov.hmcts.payment.api.exceptions;

public class ServiceRequestCancellationException extends RuntimeException {

    public ServiceRequestCancellationException(String message) {
        super(message);
    }

    public ServiceRequestCancellationException() {
        this("Service request cancelled");
    }
}
