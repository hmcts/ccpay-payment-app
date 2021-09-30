package uk.gov.hmcts.payment.api.v1.model.exceptions;

public class ServiceRequestExceptionForNoAmountDue extends RuntimeException {
    public ServiceRequestExceptionForNoAmountDue(String message) {
        super(message);
    }
}
