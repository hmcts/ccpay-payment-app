package uk.gov.hmcts.payment.api.exception;

public class InvalidPartialRefundRequestException extends RuntimeException {

    public InvalidPartialRefundRequestException(String message) {
        super(message);
    }
}
