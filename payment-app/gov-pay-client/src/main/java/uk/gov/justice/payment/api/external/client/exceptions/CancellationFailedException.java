package uk.gov.justice.payment.api.external.client.exceptions;

import uk.gov.justice.payment.api.external.client.dto.Error;

public class CancellationFailedException extends GovPayException {

    public CancellationFailedException(Error error) {
        super(error);
    }
}
