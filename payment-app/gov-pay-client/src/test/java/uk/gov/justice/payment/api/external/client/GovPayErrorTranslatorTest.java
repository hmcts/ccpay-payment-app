package uk.gov.justice.payment.api.external.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import uk.gov.justice.payment.api.external.client.dto.Error;
import uk.gov.justice.payment.api.external.client.exceptions.*;

import static org.assertj.core.api.Assertions.assertThat;

public class GovPayErrorTranslatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GovPayErrorTranslator translator = new GovPayErrorTranslator(objectMapper);

    @Test
    public void translatesAsExpected() throws JsonProcessingException {
        assertThat(exceptionClassForErrorCode("P0198")).isEqualTo(DownstreamSystemErrorException.class);
        assertThat(exceptionClassForErrorCode("P0199")).isEqualTo(AccountErrorException.class);
        assertThat(exceptionClassForErrorCode("P0200")).isEqualTo(PaymentNotFoundException.class);
        assertThat(exceptionClassForErrorCode("P0298")).isEqualTo(DownstreamSystemErrorException.class);
        assertThat(exceptionClassForErrorCode("P0300")).isEqualTo(PaymentNotFoundException.class);
        assertThat(exceptionClassForErrorCode("P0398")).isEqualTo(DownstreamSystemErrorException.class);
        assertThat(exceptionClassForErrorCode("P0498")).isEqualTo(DownstreamSystemErrorException.class);
        assertThat(exceptionClassForErrorCode("P0500")).isEqualTo(PaymentNotFoundException.class);
        assertThat(exceptionClassForErrorCode("P0501")).isEqualTo(CancellationFailedException.class);
        assertThat(exceptionClassForErrorCode("P0598")).isEqualTo(DownstreamSystemErrorException.class);
        assertThat(exceptionClassForErrorCode("P0900")).isEqualTo(TooManyRequestsException.class);
        assertThat(exceptionClassForErrorCode("P0999")).isEqualTo(GovPayUnavailableException.class);
    }

    @Test
    public void unmappedErrorCode() throws JsonProcessingException {
        assertThat(exceptionClassForErrorCode("-1")).isEqualTo(UnmappedGovPayErrorException.class);
    }

    @Test(expected = RuntimeException.class)
    public void invalidResponse() throws JsonProcessingException {
        translator.toException(objectMapper.writeValueAsBytes(new byte[0]));
    }

    private Class exceptionClassForErrorCode(String errorCode) throws JsonProcessingException {
        Error error = new Error(errorCode, "");
        return translator.toException(objectMapper.writeValueAsBytes(error)).getClass();
    }

}