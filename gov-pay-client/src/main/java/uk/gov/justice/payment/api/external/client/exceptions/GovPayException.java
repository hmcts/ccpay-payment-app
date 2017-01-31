package uk.gov.justice.payment.api.external.client.exceptions;

import uk.gov.justice.payment.api.external.client.dto.Error;

public class GovPayException extends RuntimeException {
    private final Error error;

    public GovPayException(Error error) {
        this.error = error;
    }
}
