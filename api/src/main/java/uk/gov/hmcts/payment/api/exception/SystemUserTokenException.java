package uk.gov.hmcts.payment.api.exception;

/**
 * Thrown when an IDAM access token cannot be obtained for the system user.
 */
public class SystemUserTokenException extends RuntimeException {

    public SystemUserTokenException(String message) {
        super(message);
    }

    public SystemUserTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
