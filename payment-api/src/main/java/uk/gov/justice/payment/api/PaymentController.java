package uk.gov.justice.payment.api;

/**
 * Created by zeeshan on 30/08/2016.
 */


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.justice.payment.api.json.external.*;
import uk.gov.justice.payment.api.json.internal.CreatePaymentRequest;
import uk.gov.justice.payment.api.json.internal.CreatePaymentResponse;


@RestController
public class PaymentController {

    private static final Logger logger = LoggerFactory
            .getLogger(PaymentController.class);

    @Value("${gov.pay.auth.key}")
    private String authKey;

    @Autowired
    private RestTemplate restTemplate;


    @Autowired
    ObjectMapper mapper;


    @Value("${gov.pay.url}")
    private String url;

    private String BEARER = "Bearer ";;



    @ApiOperation(value = "Create payment", notes = "Create payment")
    @ApiResponses(value = {

            @ApiResponse(code = 201, message = "Payment has been created"),
            @ApiResponse(code = 400, message = "The server cannot process the request due to a client error, eg missing details in the request or a failed payment cancellation"),
            @ApiResponse(code = 401, message = "Required authentication has failed or not been provided"),
            @ApiResponse(code = 422, message = "Invalid attribute value: description. Must be less than or equal to 255 characters length"),
            @ApiResponse(code = 500, message = "Something is wrong with services")
    })
    @RequestMapping(value = "/payments", method=RequestMethod.POST)
    public ResponseEntity<CreatePaymentResponse> createPayment(@ApiParam(value = "payment request body") @RequestBody CreatePaymentRequest payload) {
        try {
            logger.debug("createPaymentRequest : " + getJson(payload));

            uk.gov.justice.payment.api.json.external.CreatePaymentRequest paymentRequest = new uk.gov.justice.payment.api.json.external.CreatePaymentRequest();
            paymentRequest.setAmount(payload.getAmount());
            paymentRequest.setReference(payload.getPaymentReference());
            paymentRequest.setDescription(payload.getDescription());
            paymentRequest.setReturnUrl(payload.getReturnUrl());

            restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(HttpHeaders.AUTHORIZATION, BEARER + authKey);
            HttpEntity<uk.gov.justice.payment.api.json.external.CreatePaymentRequest> entity = new HttpEntity<uk.gov.justice.payment.api.json.external.CreatePaymentRequest>(paymentRequest, headers);

            ResponseEntity<uk.gov.justice.payment.api.json.external.CreatePaymentResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity, uk.gov.justice.payment.api.json.external.CreatePaymentResponse.class);

            CreatePaymentResponse createPaymentResponse = new CreatePaymentResponse();
            createPaymentResponse.setPaymentId(response.getBody().getPaymentId());
            LinksInternal linksInternal = new LinksInternal();
            linksInternal.setNextUrl(response.getBody().getLinks().getNextUrl());
            linksInternal.setNextUrlPost(response.getBody().getLinks().getNextUrlPost());
            createPaymentResponse.setLinks(linksInternal);
            logger.debug("createPaymentResponse : " + getJson(createPaymentResponse));
            ResponseEntity<CreatePaymentResponse> responseEntity =  new ResponseEntity<CreatePaymentResponse>(createPaymentResponse,
                    response.getStatusCode());
            return responseEntity;
        } catch (HttpClientErrorException e) {
            e.printStackTrace();
            logger.debug("createPaymentResponse : Error " + e.getMessage());
            return new ResponseEntity(e.getResponseBodyAsString(), e.getStatusCode());
        }
    }

    @ApiOperation(value = "Get payment details by id", notes = "Get payment details for supplied payment id")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Payment information request succeeded"),
            @ApiResponse(code = 401, message = "Credentials are required to access this resource"),
            @ApiResponse(code = 404, message = "The resource you want cannot be found"),
            @ApiResponse(code = 500, message = "Something is wrong with services")
    })
    @RequestMapping(value="/payments/{paymentId}", method=RequestMethod.GET)
    public ResponseEntity<ViewPaymentResponse> viewPayment(@ApiParam(value = "Payment id") @PathVariable("paymentId") String paymentId)  {
        try {
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(HttpHeaders.AUTHORIZATION, BEARER +authKey);
            HttpEntity entity = new HttpEntity(headers);
            ResponseEntity<ViewPaymentResponse> response = restTemplate.exchange(url+"/"+paymentId, HttpMethod.GET ,entity, ViewPaymentResponse.class);
            logger.debug("viewPaymentResponse : " + getJson(response));
            return response;
        } catch (HttpClientErrorException e) {
            logger.debug("viewPaymentResponse : Error " + e.getMessage());
            return new ResponseEntity(e.getResponseBodyAsString(), e.getStatusCode());
        }
    }

    private String getJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "";
        }

    }
}