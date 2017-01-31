package uk.gov.justice.payment.api.external.client.exceptions;

import uk.gov.justice.payment.api.external.client.dto.Error;

public class GovPayAccountErrorException extends GovPayException {

    public GovPayAccountErrorException(Error error) {
        super(error);
    }
}
