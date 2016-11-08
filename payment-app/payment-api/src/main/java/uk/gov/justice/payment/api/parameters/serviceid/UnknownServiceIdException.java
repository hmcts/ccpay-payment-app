package uk.gov.justice.payment.api.parameters.serviceid;

public class UnknownServiceIdException extends RuntimeException {
    UnknownServiceIdException(String serviceId) {
        super("Unknown service id provided: " + serviceId);
    }
}
