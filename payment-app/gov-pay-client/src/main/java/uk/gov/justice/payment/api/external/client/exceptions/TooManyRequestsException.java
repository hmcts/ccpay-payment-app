package uk.gov.justice.payment.api.external.client.exceptions;

import uk.gov.justice.payment.api.external.client.dto.Error;

public class TooManyRequestsException extends GovPayException {

    public TooManyRequestsException(Error error) {
        super(error);
    }
}
