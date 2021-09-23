package uk.gov.hmcts.payment.api.exception;

public class SendMessageTopicFailedException extends RuntimeException {
    public SendMessageTopicFailedException (String message) {
        super(message);
    }
}
