package uk.gov.hmcts.payment.api.external.client.exceptions;

import uk.gov.hmcts.payment.api.external.client.dto.Error;

public class GovPayTooManyRequestsException extends GovPayException {

    public GovPayTooManyRequestsException(Error error) {
        super(error);
    }
}
