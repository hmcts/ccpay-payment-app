package uk.gov.hmcts.payment.api.external.client.exceptions;

import uk.gov.hmcts.payment.api.external.client.dto.Error;

public class GovPayPaymentNotFoundException extends GovPayException {

    public GovPayPaymentNotFoundException(Error error) {
        super(error);
    }
}
