package uk.gov.justice.payment.api;

/**
 * Created by zeeshan on 30/08/2016.
 */


import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import uk.gov.justice.payment.api.json.CreatePaymentRequest;


@RestController
public class PaymentController {

    @Value("${auth.key}")
    private String authKey;

    @Value("${url}")
    private String url;

    @RequestMapping("/payment")
    public String createPayment() throws JsonProcessingException {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer "+authKey);
        CreatePaymentRequest paymentRequest = new CreatePaymentRequest();
        paymentRequest.setAmount(10);
        paymentRequest.setDescription("Test payment description.");
        paymentRequest.setReference("XXX-XXX-XXX-XXX");
        paymentRequest.setReturnUrl("https://www.google.com");
        ObjectMapper mapper = new ObjectMapper();
        String requestJson = mapper.writeValueAsString(paymentRequest);
        HttpEntity<String> entity = new HttpEntity<String>(requestJson,headers);
        String result = restTemplate.postForObject(url, entity, String.class);
        return result;
    }
}