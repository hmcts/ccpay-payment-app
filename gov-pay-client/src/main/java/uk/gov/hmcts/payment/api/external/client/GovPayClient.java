package uk.gov.hmcts.payment.api.external.client;

import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.RefundPaymentRequest;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
public class GovPayClient {

    private final String url;
    private final RestTemplate restTemplate;
    private final GovPayErrorTranslator errorTranslator;

    @Autowired
    public GovPayClient(@Value("#{${payment.testing-support.enabled} ? '${payment.testing-support.wiremock.url}' : '${gov.pay.url}'}") String url,
                        RestTemplate restTemplate,
                        GovPayErrorTranslator errorTranslator) {
        this.url = url;
        this.restTemplate = restTemplate;
        this.errorTranslator = errorTranslator;
    }

    public GovPayPayment createPayment(String authorizationKey, CreatePaymentRequest createPaymentRequest) {
        return doWithErrorTranslation(() -> {
            HttpEntity<CreatePaymentRequest> entity = new HttpEntity<>(createPaymentRequest, getHeaders(authorizationKey));
            ResponseEntity<GovPayPayment> response = restTemplate.exchange(url, POST, entity, GovPayPayment.class);
            return response.getBody();
        });
    }

    public GovPayPayment retrievePayment(String authorizationKey, String govPayId) {
        return doWithErrorTranslation(() -> {
            ResponseEntity<GovPayPayment> response = restTemplate.exchange(url + "/" + govPayId, GET, new HttpEntity(getHeaders(authorizationKey)), GovPayPayment.class);
            return response.getBody();
        });
    }

    public void cancelPayment(String authorizationKey, String cancelUrl) {
        doWithErrorTranslation(() -> restTemplate.exchange(cancelUrl, POST, new HttpEntity(getHeaders(authorizationKey)), Void.class));
    }

    public void refundPayment(String authorizationKey, String refundsUrl, RefundPaymentRequest refundPaymentRequest) {
        doWithErrorTranslation(() -> restTemplate.exchange(refundsUrl, POST, new HttpEntity(refundPaymentRequest, getHeaders(authorizationKey)), Void.class));
    }

    private <T> T doWithErrorTranslation(Supplier<T> function) {
        try {
            return function.get();
        } catch (HttpClientErrorException e) {
            throw errorTranslator.toException(e.getResponseBodyAsByteArray());
        }
    }

    private HttpHeaders getHeaders(String authorizationKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);
        headers.set(AUTHORIZATION, "Bearer " + authorizationKey);
        return headers;
    }
}
