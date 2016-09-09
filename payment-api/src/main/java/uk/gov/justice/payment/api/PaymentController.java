package uk.gov.justice.payment.api;

/**
 * Created by zeeshan on 30/08/2016.
 */


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpsParameters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.justice.payment.api.json.CreatePaymentRequest;
import uk.gov.justice.payment.api.json.CreatePaymentResponse;
import uk.gov.justice.payment.api.json.ViewPaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
public class PaymentController {

    private static final Logger logger = LoggerFactory
            .getLogger(PaymentController.class);



    @Value("${gov.pay.auth.key}")
    private String authKey;

    @Value("${gov.pay.url}")
    private String url;
    private String BEARER = "Bearer ";;

    @RequestMapping(value = "/payments", method=RequestMethod.POST)
    public CreatePaymentResponse createPayment(@RequestBody CreatePaymentRequest createPaymentRequest) {
        logger.debug("Request : " + getJson(createPaymentRequest));
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, BEARER +authKey);
        HttpEntity<CreatePaymentRequest> entity = new HttpEntity<CreatePaymentRequest>(createPaymentRequest,headers);
        CreatePaymentResponse response = restTemplate.postForObject(url, entity, CreatePaymentResponse.class);
        logger.debug("Response : " + getJson(response));
        return response;
    }

    @RequestMapping(value="/payments/{paymentId}", method=RequestMethod.GET)
    public ResponseEntity<ViewPaymentResponse> viewPayment(@PathVariable("paymentId") String paymentId)  {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, BEARER +authKey);
        HttpEntity entity = new HttpEntity(headers);
        ResponseEntity<ViewPaymentResponse> response = restTemplate.exchange(url+"/"+paymentId, HttpMethod.GET ,entity, ViewPaymentResponse.class);
        logger.debug("Response : " + getJson(response));
        return response;
    }

    private String getJson(Object obj) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "";
    }
}