package uk.gov.justice.payment.api.external.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.justice.payment.api.external.client.dto.Error;
import uk.gov.justice.payment.api.external.client.exceptions.*;

import java.io.IOException;

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
     * @see https://gds-payments.gelato.io/docs/versions/1.0.0/api-reference
     */
    GovPayException toException(byte[] responseBody) {
        try {
            Error error = objectMapper.readValue(responseBody, Error.class);

            switch (error.getCode()) {
                case "P0198":
                    return new DownstreamSystemErrorException(error);
                case "P0199":
                    return new AccountErrorException(error);
                case "P0200":
                    return new PaymentNotFoundException(error);
                case "P0298":
                    return new DownstreamSystemErrorException(error);
                case "P0300":
                    return new PaymentNotFoundException(error);
                case "P0398":
                    return new DownstreamSystemErrorException(error);
                case "P0498":
                    return new DownstreamSystemErrorException(error);
                case "P0500":
                    return new PaymentNotFoundException(error);
                case "P0501":
                    return new CancellationFailedException(error);
                case "P0598":
                    return new DownstreamSystemErrorException(error);
                case "P0900":
                    return new TooManyRequestsException(error);
                case "P0999":
                    return new GovPayUnavailableException(error);

                default:
                    return new UnmappedGovPayErrorException(error);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to parse error body");
        }
    }
}
