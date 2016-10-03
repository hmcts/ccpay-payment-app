package uk.gov.justice.payment.api;

/**
 * Created by zeeshan on 30/08/2016.
 */


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.justice.payment.api.json.api.CreatePaymentRequest;
import uk.gov.justice.payment.api.json.api.CreatePaymentResponse;
import uk.gov.justice.payment.api.json.api.ViewPaymentResponse;
import uk.gov.justice.payment.api.json.external.GDSCreatePaymentRequest;
import uk.gov.justice.payment.api.json.external.GDSCreatePaymentResponse;
import uk.gov.justice.payment.api.json.external.GDSViewPaymentResponse;
import uk.gov.justice.payment.api.services.PaymentService;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;


@RestController
@Api(value = "/payment", description = "Payment REST API")
public class PaymentController {

    private static final Logger logger = LoggerFactory
            .getLogger(PaymentController.class);

    @Value("${gov.pay.auth.key}")
    private String authKey;

    @Autowired
    private RestTemplate restTemplate;


    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private PaymentService paymentService;

    @Value("${gov.pay.url}")
    private String url;

    private String BEARER = "Bearer ";
    private HttpHeaders headers;

    @ApiOperation(value = "Create payment", notes = "Create payment")
    @ApiResponses(value = {

            @ApiResponse(code = 201, message = "Payment has been created"),
            @ApiResponse(code = 400, message = "The server cannot process the request due to a client error, eg missing details in the request."),
            @ApiResponse(code = 401, message = "Required authentication has failed or not been provided"),
            @ApiResponse(code = 422, message = "Invalid or missing attribute"),
            @ApiResponse(code = 500, message = "Something is wrong with services")
    })
    @RequestMapping(value = "/payments", method=RequestMethod.POST)
    public ResponseEntity<CreatePaymentResponse> createPayment(@ApiParam(value = "payment request body") @RequestBody(required = true) CreatePaymentRequest payload,
                                                               HttpServletRequest httpServletRequest) {
        try {
            logger.debug("createPaymentRequest : " + getJson(payload));
            if(!payload.isValid()) {
                return new ResponseEntity(payload.getValidationMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
            }
            GDSCreatePaymentRequest paymentRequest = new GDSCreatePaymentRequest(payload);
            HttpEntity<GDSCreatePaymentRequest> entity = new HttpEntity<GDSCreatePaymentRequest>(paymentRequest, headers);
            logger.debug("GDS : createPaymentRequest : " + getJson(paymentRequest));
            ResponseEntity<GDSCreatePaymentResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity, GDSCreatePaymentResponse.class);
            String url = httpServletRequest.getRequestURL().toString();
            CreatePaymentResponse createPaymentResponse = new CreatePaymentResponse(response.getBody(),url);
            paymentService.storePayment(payload,response.getBody());
            logger.debug("GDS : createPaymentResponse : " + getJson(createPaymentResponse));
            ResponseEntity<CreatePaymentResponse> responseEntity =  new ResponseEntity<CreatePaymentResponse>(createPaymentResponse,response.getStatusCode());
            return responseEntity;
        } catch (HttpClientErrorException e) {
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
            HttpEntity entity = new HttpEntity(headers);
            ResponseEntity<GDSViewPaymentResponse> response = restTemplate.exchange(url+"/"+paymentId, HttpMethod.GET ,entity, GDSViewPaymentResponse.class);
            logger.debug("GDS : viewPaymentResponse : " + getJson(response));
            ViewPaymentResponse viewPaymentResponse = new ViewPaymentResponse(response.getBody());
            ResponseEntity<ViewPaymentResponse> responseEntity =  new ResponseEntity<ViewPaymentResponse>(viewPaymentResponse , response.getStatusCode());
            return responseEntity;
        } catch (HttpClientErrorException e) {
            logger.debug("viewPaymentResponse : Error " + e.getMessage());
            return new ResponseEntity(e.getResponseBodyAsString(), e.getStatusCode());
        }
    }


    @ApiOperation(value = "Cancel payment", notes = "Cancel payment for supplied payment id")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Payment cancel request succeeded"),
            @ApiResponse(code = 400, message = "No payment matched the paymentId you provided"),
            @ApiResponse(code = 404, message = "The Payment you want cannot be found"),
            @ApiResponse(code = 500, message = "Something is wrong with services"),
    })
    @RequestMapping(value="/payments/{paymentId}/cancel", method=RequestMethod.POST)
    public ResponseEntity<String> cancelPayment(@ApiParam(value = "Payment id") @PathVariable("paymentId") String paymentId)  {

        try {
            logger.debug("GDS : cancelPayment : paymentId=" + paymentId);
            HttpEntity entity = new HttpEntity(headers);
            ResponseEntity<String> response = restTemplate.exchange(url+"/"+paymentId+"/cancel", HttpMethod.POST ,entity, String.class);
            logger.debug("GDS : cancelPaymentResponse : " + response);
            return response;
        } catch (HttpClientErrorException e) {
            logger.debug("viewPaymentResponse : Error " + e.getMessage());
            return new ResponseEntity(e.getResponseBodyAsString(), e.getStatusCode());
        }
    }


    @PostConstruct
    private void init() {
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, BEARER + authKey);
    }


    private String getJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "";
        }

    }
}