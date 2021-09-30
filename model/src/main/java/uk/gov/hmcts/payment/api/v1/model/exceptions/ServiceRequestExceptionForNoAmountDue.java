package uk.gov.hmcts.payment.api.v1.model.exceptions;

public class ServiceRequestExceptionForNoAmountDue extends PaymentException {

    public ServiceRequestExceptionForNoAmountDue() {}

    public ServiceRequestExceptionForNoAmountDue(String message) {
        super(message);
    }

}
