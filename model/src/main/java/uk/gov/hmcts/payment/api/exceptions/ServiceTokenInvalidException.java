package uk.gov.hmcts.payment.api.exceptions;

public class ServiceTokenInvalidException extends RuntimeException{

    public ServiceTokenInvalidException(String message) {
        super(message);
    }
}
