package uk.gov.hmcts.payment.api.exceptions;

public class ServiceRequestReferenceNotFoundException extends RuntimeException {
    public ServiceRequestReferenceNotFoundException(String message) {
        super(message);
    }
}
