package uk.gov.justice.payment.api.external.client.exceptions;


import uk.gov.justice.payment.api.external.client.dto.Error;

public class UnmappedGovPayErrorException extends GovPayException {
    public UnmappedGovPayErrorException(Error error) {
        super(error);
    }
}
