package uk.gov.hmcts.payment.api.external.client.exceptions;

import uk.gov.hmcts.payment.api.external.client.dto.Error;

public class GovPayUnauthorizedClientException extends GovPayException {

    public GovPayUnauthorizedClientException(Error error) {
        super(error);
    }
}
