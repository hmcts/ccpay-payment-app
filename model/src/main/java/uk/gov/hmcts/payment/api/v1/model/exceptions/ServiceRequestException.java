package uk.gov.hmcts.payment.api.v1.model.exceptions;

public class ServiceRequestException extends PaymentException {

    public ServiceRequestException() {}

    public ServiceRequestException(String message) {
        super(message);
    }

}
