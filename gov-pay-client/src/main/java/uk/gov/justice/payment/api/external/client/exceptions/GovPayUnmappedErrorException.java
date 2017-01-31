package uk.gov.justice.payment.api.external.client.exceptions;


import uk.gov.justice.payment.api.external.client.dto.Error;

public class GovPayUnmappedErrorException extends GovPayException {
    public GovPayUnmappedErrorException(Error error) {
        super(error);
    }
}
