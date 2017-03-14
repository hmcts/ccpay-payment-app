package uk.gov.hmcts.payment.api.external.client.exceptions;

import uk.gov.hmcts.payment.api.external.client.dto.Error;

public class GovPayCancellationFailedException extends GovPayException {

    public GovPayCancellationFailedException(Error error) {
        super(error);
    }
}
