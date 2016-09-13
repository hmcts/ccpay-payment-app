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
import io.swagger.annotations.*;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponses;
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

    @ApiOperation(value = "Create payment", notes = "Create payment")

    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successfully created payment"),
            @ApiResponse(code = 500, message = "Internal server error", response = java.lang.String.class)
    })
    @RequestMapping(value = "/payments", method=RequestMethod.POST)
    public ResponseEntity<CreatePaymentResponse> createPayment(@ApiParam(value = "payment request body") @RequestBody CreatePaymentRequest payload) {
        logger.debug("createPaymentRequest : " + getJson(payload));
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, BEARER +authKey);
        HttpEntity<CreatePaymentRequest> entity = new HttpEntity<CreatePaymentRequest>(payload,headers);
        ResponseEntity<CreatePaymentResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity, CreatePaymentResponse.class);
        logger.debug("createPaymentResponse : " + getJson(response));
        if(HttpStatus.CREATED.equals(response.getStatusCode())) {
            return response;
        } else {
            return new ResponseEntity(null,HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @ApiOperation(value = "Get payment details by id", notes = "Get payment details for supplied payment id")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully retrieved payment details"),
            @ApiResponse(code = 404, message = "Payment not found"),
            @ApiResponse(code = 500, message = "Internal server error", response = java.lang.String.class)
    })
    @RequestMapping(value="/payments/{paymentId}", method=RequestMethod.GET)
    public ResponseEntity<ViewPaymentResponse> viewPayment(@ApiParam(value = "Payment id") @PathVariable("paymentId") String paymentId)  {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, BEARER +authKey);
        HttpEntity entity = new HttpEntity(headers);
        ResponseEntity<ViewPaymentResponse> response = restTemplate.exchange(url+"/"+paymentId, HttpMethod.GET ,entity, ViewPaymentResponse.class);
        logger.debug("viewPaymentResponse : " + getJson(response));
        if(HttpStatus.OK.equals(response.getStatusCode())) {
            return response;
        } else {
            return new ResponseEntity(null,HttpStatus.INTERNAL_SERVER_ERROR);
        }
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