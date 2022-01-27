package uk.gov.hmcts.payment.api.exception;

public class InvalidRefundRequestException extends RuntimeException {
    
    public InvalidRefundRequestException(String message) {
        super(message);
    }

}
