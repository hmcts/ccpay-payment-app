package uk.gov.hmcts.payment.api.v1.model.exceptions;

public class ServiceRequestExceptionForNoMatchingAmount extends RuntimeException {
    public ServiceRequestExceptionForNoMatchingAmount() {
    }

    public ServiceRequestExceptionForNoMatchingAmount(String message) {
        super(message);
    }
}
