package uk.gov.hmcts.payment.api.external.client;

import tools.jackson.databind.ObjectMapper;
import org.junit.Test;
import uk.gov.hmcts.payment.api.external.client.dto.Error;
import uk.gov.hmcts.payment.api.external.client.exceptions.*;

import static org.assertj.core.api.Assertions.assertThat;

public class GovPayErrorTranslatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GovPayErrorTranslator translator = new GovPayErrorTranslator(objectMapper);

    @Test
    public void translatesAsExpected() {
        assertThat(exceptionClassForErrorCode("P0198")).isEqualTo(GovPayDownstreamSystemErrorException.class);
        assertThat(exceptionClassForErrorCode("P0199")).isEqualTo(GovPayAccountErrorException.class);
        assertThat(exceptionClassForErrorCode("P0200")).isEqualTo(GovPayPaymentNotFoundException.class);
        assertThat(exceptionClassForErrorCode("P0298")).isEqualTo(GovPayDownstreamSystemErrorException.class);
        assertThat(exceptionClassForErrorCode("P0300")).isEqualTo(GovPayPaymentNotFoundException.class);
        assertThat(exceptionClassForErrorCode("P0398")).isEqualTo(GovPayDownstreamSystemErrorException.class);
        assertThat(exceptionClassForErrorCode("P0498")).isEqualTo(GovPayDownstreamSystemErrorException.class);
        assertThat(exceptionClassForErrorCode("P0500")).isEqualTo(GovPayPaymentNotFoundException.class);
        assertThat(exceptionClassForErrorCode("P0501")).isEqualTo(GovPayCancellationFailedException.class);
        assertThat(exceptionClassForErrorCode("P0598")).isEqualTo(GovPayDownstreamSystemErrorException.class);
        assertThat(exceptionClassForErrorCode("P0600")).isEqualTo(GovPayPaymentNotFoundException.class);
        assertThat(exceptionClassForErrorCode("P0603")).isEqualTo(GovPayRefundNotAvailableException.class);
        assertThat(exceptionClassForErrorCode("P0604")).isEqualTo(GovPayRefundAmountMismatch.class);
        assertThat(exceptionClassForErrorCode("P0900")).isEqualTo(GovPayTooManyRequestsException.class);
        assertThat(exceptionClassForErrorCode("P0999")).isEqualTo(GovPayUnavailableException.class);
    }

    @Test
    public void unmappedErrorCode() {
        assertThat(exceptionClassForErrorCode("-1")).isEqualTo(GovPayUnmappedErrorException.class);
    }

    @Test(expected = RuntimeException.class)
    public void invalidResponse() {
        translator.toException(objectMapper.writeValueAsBytes(new byte[0]));
    }

    private Class exceptionClassForErrorCode(String errorCode) {
        Error error = new Error(errorCode, "");
        return translator.toException(objectMapper.writeValueAsBytes(error)).getClass();
    }

}
