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
import uk.gov.justice.payment.api.json.CreatePaymentRequest;
import uk.gov.justice.payment.api.json.CreatePaymentResponse;
import uk.gov.justice.payment.api.json.ViewPaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
public class PaymentController {

    private static final Logger logger = LoggerFactory
            .getLogger(PaymentController.class);

    @Value("${auth.key}")
    private String authKey;

    @Value("${url}")
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
        CreatePaymentRequest paymentRequest = new CreatePaymentRequest();
        paymentRequest.setAmount(10);
        paymentRequest.setDescription("Test payment description.");
        paymentRequest.setReference("XXX-XXX-XXX-XXX");
        paymentRequest.setReturnUrl("https://www.google.com");
        HttpEntity<CreatePaymentRequest> entity = new HttpEntity<CreatePaymentRequest>(createPaymentRequest,headers);
        CreatePaymentResponse response = restTemplate.postForObject(url, entity, CreatePaymentResponse.class);
        logger.debug("Response : " + getJson(response));
        return response;
    }
    @RequestMapping(value="/payments", method=RequestMethod.GET)

    public ResponseEntity<ViewPaymentResponse> viewPayment()  {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, BEARER +authKey);
        Map<String, String> params = new HashMap<String, String>();
        params.put("paymentId", "n657stl3mlh7ne6joij3jk7fau");
        HttpEntity entity = new HttpEntity(headers);
        ResponseEntity<ViewPaymentResponse> response = restTemplate.exchange(url, HttpMethod.GET ,entity, ViewPaymentResponse.class,params);
        logger.debug("status="+response.getBody().getResults().get(0).getState().getStatus());
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