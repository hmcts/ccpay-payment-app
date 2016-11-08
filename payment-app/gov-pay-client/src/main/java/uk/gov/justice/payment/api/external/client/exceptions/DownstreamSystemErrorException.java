package uk.gov.justice.payment.api.external.client.exceptions;

import uk.gov.justice.payment.api.external.client.dto.Error;

public class DownstreamSystemErrorException extends GovPayException {

    public DownstreamSystemErrorException(Error error) {
        super(error);
    }
}
