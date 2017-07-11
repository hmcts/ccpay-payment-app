package uk.gov.hmcts.payment.api.external.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.RefundPaymentRequest;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayClientException;

import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

@Component
@SuppressWarnings(value = "HTTP_PARAMETER_POLLUTION", justification = "No way around it in a client")
public class GovPayClient {

    private final String url;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final GovPayErrorTranslator errorTranslator;

    @Autowired
    public GovPayClient(@Value("${gov.pay.url}") String url,
                        HttpClient httpClient,
                        ObjectMapper objectMapper,
                        GovPayErrorTranslator errorTranslator) {
        this.url = url;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.errorTranslator = errorTranslator;
    }

    public GovPayPayment createPayment(String authorizationKey, CreatePaymentRequest createPaymentRequest) {
        return withIOExceptionHandling(() -> {
            HttpPost request = postRequestFor(authorizationKey, url, createPaymentRequest);
            HttpResponse response = httpClient.execute(request);
            checkNotAnError(response);
            return objectMapper.readValue(response.getEntity().getContent(), GovPayPayment.class);
        });
    }

    public GovPayPayment retrievePayment(String authorizationKey, String govPayId) {
        return withIOExceptionHandling(() -> {
            HttpGet request = getRequestFor(authorizationKey, url + "/" + govPayId);
            HttpResponse response = httpClient.execute(request);
            checkNotAnError(response);
            return objectMapper.readValue(response.getEntity().getContent(), GovPayPayment.class);
        });
    }

    public void cancelPayment(String authorizationKey, String cancelUrl) {
        withIOExceptionHandling(() -> {
            HttpPost request = postRequestFor(authorizationKey, cancelUrl, "");
            HttpResponse response = httpClient.execute(request);
            checkNotAnError(response);
            return null;
        });
    }

    public void refundPayment(String authorizationKey, String refundsUrl, RefundPaymentRequest refundPaymentRequest) {
        withIOExceptionHandling(() -> {
            HttpPost request = postRequestFor(authorizationKey, refundsUrl, refundPaymentRequest);
            HttpResponse response = httpClient.execute(request);
            checkNotAnError(response);
            return null;
        });
    }

    private HttpPost postRequestFor(String authorizationKey, String url, Object entity) throws JsonProcessingException {
        HttpPost request = new HttpPost(url);
        request.setEntity(new StringEntity(objectMapper.writeValueAsString(entity), APPLICATION_JSON));
        request.addHeader(authorizationHeader(authorizationKey));
        return request;
    }

    private HttpGet getRequestFor(String authorizationKey, String url) {
        HttpGet request = new HttpGet(url);
        request.addHeader(CONTENT_TYPE, APPLICATION_JSON.toString());
        request.addHeader(authorizationHeader(authorizationKey));
        return request;
    }

    private Header authorizationHeader(String authorizationKey) {
        return new BasicHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authorizationKey);
    }

    private void checkNotAnError(HttpResponse httpResponse) throws IOException {
        int status = httpResponse.getStatusLine().getStatusCode();

        if (status >= 400) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            httpResponse.getEntity().writeTo(bos);
            throw errorTranslator.toException(bos.toByteArray());
        }
    }

    private <T> T withIOExceptionHandling(CheckedExceptionProvider<T> provider) {
        try {
            return provider.get();
        } catch (IOException e) {
            throw new GovPayClientException(e);
        }
    }

    private interface CheckedExceptionProvider<T> {
        T get() throws IOException;
    }
}
