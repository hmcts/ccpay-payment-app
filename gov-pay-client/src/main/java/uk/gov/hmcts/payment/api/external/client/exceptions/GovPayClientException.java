package uk.gov.hmcts.payment.api.external.client.exceptions;

public class GovPayClientException extends RuntimeException {
    public GovPayClientException(Throwable cause) {
        super(cause);
    }
}
