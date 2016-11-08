package uk.gov.justice.payment.api.external.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.justice.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.justice.payment.api.external.client.dto.Payment;

import java.util.function.Supplier;

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
    public GovPayClient(@Value("${gov.pay.url}") String url, RestTemplate restTemplate, GovPayErrorTranslator errorTranslator) {
        this.url = url;
        this.restTemplate = restTemplate;
        this.errorTranslator = errorTranslator;
    }

    public Payment createPayment(String authorizationKey, CreatePaymentRequest paymentRequestDto) {
        return doWithErrorTranslation(() -> {
            HttpEntity<CreatePaymentRequest> entity = new HttpEntity<>(paymentRequestDto, getHeaders(authorizationKey));
            ResponseEntity<Payment> response = restTemplate.exchange(url, POST, entity, Payment.class);
            return response.getBody();
        });
    }

    public Payment getPayment(String authorizationKey, String paymentId) {
        return doWithErrorTranslation(() -> {
            ResponseEntity<Payment> response = restTemplate.exchange(url + "/" + paymentId, GET, new HttpEntity(getHeaders(authorizationKey)), Payment.class);
            return response.getBody();
        });
    }

    public void cancelPayment(String authorizationKey, String paymentId) {
        doWithErrorTranslation(() -> restTemplate.exchange(url + "/" + paymentId + "/cancel", POST, new HttpEntity(getHeaders(authorizationKey)), Payment.class));
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
