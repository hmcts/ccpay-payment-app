package uk.gov.justice.payment.api.external.client.exceptions;

import uk.gov.justice.payment.api.external.client.dto.Error;

public class PaymentNotFoundException extends GovPayException {

    public PaymentNotFoundException(Error error) {
        super(error);
    }
}
