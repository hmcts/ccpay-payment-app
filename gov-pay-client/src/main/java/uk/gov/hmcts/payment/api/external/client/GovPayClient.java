package uk.gov.hmcts.payment.api.external.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.Header;
import java.io.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.RefundPaymentRequest;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayClientException;

import java.io.IOException;

import static org.apache.hc.core5.http.ContentType.APPLICATION_JSON;
import static org.apache.hc.core5.http.HttpHeaders.CONTENT_TYPE;


@Component
@SuppressWarnings(value = "HTTP_PARAMETER_POLLUTION", justification = "No way around it in a client")
public class GovPayClient {
    private static final Logger LOG = LoggerFactory.getLogger(GovPayClient.class);
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

    @CircuitBreaker(name = "createCardPayment")
    public GovPayPayment createPayment(String authorizationKey, CreatePaymentRequest createPaymentRequest) {
        LOG.info("Inside createPayment in GovPayClient");
        return withIOExceptionHandling(() -> {
            HttpPost request = postRequestFor(authorizationKey, url, createPaymentRequest);
            ClassicHttpResponse response = (ClassicHttpResponse) httpClient.execute(request);
            LOG.info("response {}", objectMapper.writeValueAsString(response.getCode()));
            checkNotAnError(response);
            String responseBody = EntityUtils.toString(response.getEntity());
            return objectMapper.readValue(responseBody, GovPayPayment.class);
        });
    }

    @CircuitBreaker(name = "retrieveCardPayment")
    public GovPayPayment retrievePayment(String authorizationKey, String govPayId) {
        LOG.info("retrievePayment");
        return withIOExceptionHandling(() -> {
            HttpGet request = getRequestFor(authorizationKey, url + "/" + govPayId);
            ClassicHttpResponse response = (ClassicHttpResponse) httpClient.execute(request);
            checkNotAnError(response);
            String responseBody = EntityUtils.toString(response.getEntity());
            return objectMapper.readValue(responseBody, GovPayPayment.class);
        });
    }

    public void cancelPayment(String authorizationKey, String cancelUrl) {
        LOG.info("CANCELLING PAYMENT");
        withIOExceptionHandling(() -> {
            HttpPost request = postRequestFor(authorizationKey, cancelUrl, null);
            ClassicHttpResponse response = (ClassicHttpResponse) httpClient.execute(request);
            checkNotAnError(response);
            return null;
        });
    }

    public void refundPayment(String authorizationKey, String refundsUrl, RefundPaymentRequest refundPaymentRequest) {
        withIOExceptionHandling(() -> {
            HttpPost request = postRequestFor(authorizationKey, refundsUrl, refundPaymentRequest);
            ClassicHttpResponse response = (ClassicHttpResponse) httpClient.execute(request);
            checkNotAnError(response);
            return null;
        });
    }

    private HttpPost postRequestFor(String authorizationKey, String url, Object entity) throws JsonProcessingException {
        LOG.info("new StringEntity(objectMapper.writeValueAsString(entity) {}", objectMapper.writeValueAsString(entity));
        return postRequestFor(authorizationKey, url, new StringEntity(objectMapper.writeValueAsString(entity), APPLICATION_JSON));
    }

    private HttpPost postRequestFor(String authorizationKey, String url, HttpEntity entity) throws JsonProcessingException {
        LOG.info("Inside postRequestFor in GovPayClient");
        LOG.info("authorizationKey {} ", authorizationKey);
        LOG.info("url {} ", url);
        try {
            LOG.info("entity {}", entity.getContent().toString());
        } catch (Exception e) {
            LOG.info(e.getMessage());
        }

        HttpPost request = new HttpPost(url);
        request.setEntity(entity);
        request.addHeader(authorizationHeader(authorizationKey));
        LOG.info("Request value: {}", request);
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

    private void checkNotAnError(ClassicHttpResponse httpResponse) throws IOException {
        int status = httpResponse.getCode();

        if (status >= 400) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            httpResponse.getEntity().writeTo(bos);
            throw errorTranslator.toException(bos.toByteArray());
        }
    }

    private <T> T withIOExceptionHandling(CheckedExceptionProvider<T> provider) {
        try {
            return provider.get();
        } catch (IOException | ParseException e) {
            throw new GovPayClientException(e);
        }
    }

    private interface CheckedExceptionProvider<T> {
        T get() throws IOException, ParseException;
    }
}