package uk.gov.hmcts.payment.api.external.client.exceptions;

import uk.gov.hmcts.payment.api.external.client.dto.Error;

public class GovPayDownstreamSystemErrorException extends GovPayException {

    public GovPayDownstreamSystemErrorException(Error error) {
        super(error);
    }
}
