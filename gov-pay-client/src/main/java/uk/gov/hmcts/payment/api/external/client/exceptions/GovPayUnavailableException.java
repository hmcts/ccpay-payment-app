package uk.gov.hmcts.payment.api.external.client.exceptions;

import uk.gov.hmcts.payment.api.external.client.dto.Error;

public class GovPayUnavailableException extends GovPayException {

    public GovPayUnavailableException(Error error) {
        super(error);
    }
}
