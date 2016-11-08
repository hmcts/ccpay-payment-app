package uk.gov.justice.payment.api.external.client.exceptions;

import uk.gov.justice.payment.api.external.client.dto.Error;

public class AccountErrorException extends GovPayException {

    public AccountErrorException(Error error) {
        super(error);
    }
}
