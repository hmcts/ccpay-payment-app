package uk.gov.hmcts.payment.api.exceptions;

public class PciPalClientException extends RuntimeException {
    public PciPalClientException(Throwable cause) {
        super(cause);
    }
}
