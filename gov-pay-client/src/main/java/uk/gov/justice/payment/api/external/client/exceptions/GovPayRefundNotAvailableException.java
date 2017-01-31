package uk.gov.justice.payment.api.external.client.exceptions;

import uk.gov.justice.payment.api.external.client.dto.Error;

public class GovPayRefundNotAvailableException extends GovPayException {
    public GovPayRefundNotAvailableException(Error error) {
        super(error);
    }
}
