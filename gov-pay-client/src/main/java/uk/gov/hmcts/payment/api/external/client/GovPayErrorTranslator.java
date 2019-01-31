package uk.gov.hmcts.payment.api.external.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.external.client.dto.Error;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayAccountErrorException;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayCancellationFailedException;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayDownstreamSystemErrorException;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayException;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayPaymentNotFoundException;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayRefundAmountMismatch;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayRefundNotAvailableException;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayTooManyRequestsException;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayUnavailableException;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayUnmappedErrorException;

@Component
public class GovPayErrorTranslator {
    private final ObjectMapper objectMapper;

    @Autowired
    public GovPayErrorTranslator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * @param responseBody response body of non 200 response
     * @return exception
     * @see <a href="https://gds-payments.gelato.io/docs/versions/1.0.0/api-reference">https://gds-payments.gelato.io/docs/versions/1.0.0/api-reference</a>
     */
    GovPayException toException(byte[] responseBody) {
        try {
            Error error = objectMapper.readValue(responseBody, Error.class);

                switch (error.getCode()) {
                case "P0198":
                    return new GovPayDownstreamSystemErrorException(error);
                case "P0199":
                    return new GovPayAccountErrorException(error);
                case "P0200":
                    return new GovPayPaymentNotFoundException(error);
                case "P0298":
                    return new GovPayDownstreamSystemErrorException(error);
                case "P0300":
                    return new GovPayPaymentNotFoundException(error);
                case "P0398":
                    return new GovPayDownstreamSystemErrorException(error);
                case "P0498":
                    return new GovPayDownstreamSystemErrorException(error);
                case "P0500":
                    return new GovPayPaymentNotFoundException(error);
                case "P0501":
                    return new GovPayCancellationFailedException(error);
                case "P0598":
                    return new GovPayDownstreamSystemErrorException(error);
                case "P0600":
                    return new GovPayPaymentNotFoundException(error);
                case "P0603":
                    return new GovPayRefundNotAvailableException(error);
                case "P0604":
                    return new GovPayRefundAmountMismatch(error);
                case "P0900":
                    return new GovPayTooManyRequestsException(error);
                case "P0999":
                    return new GovPayUnavailableException(error);

                default:
                    return new GovPayUnmappedErrorException(error);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse error body: " + new String(responseBody));
        }
    }
}
