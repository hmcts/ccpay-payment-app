package uk.gov.hmcts.payment.api.external.client.exceptions;

import uk.gov.hmcts.payment.api.external.client.dto.Error;

public class GovPayRefundAmountMismatch extends GovPayException {
    public GovPayRefundAmountMismatch(Error error) {
        super(error);
    }
}
