package uk.gov.justice.payment.api;

import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.justice.payment.api.json.api.CreatePaymentRequest;
import uk.gov.justice.payment.api.json.api.CreatePaymentResponse;
import uk.gov.justice.payment.api.json.api.TransactionRecord;
import uk.gov.justice.payment.api.json.api.ViewPaymentResponse;
import uk.gov.justice.payment.api.json.external.GDSCreatePaymentRequest;
import uk.gov.justice.payment.api.json.external.GDSCreatePaymentResponse;
import uk.gov.justice.payment.api.json.external.GDSViewPaymentResponse;
import uk.gov.justice.payment.api.services.PaymentService;
import uk.gov.justice.payment.api.services.SearchCriteria;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.*;


@RestController
@Api(value = "/payment", description = "Payment REST API")
public class PaymentController {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentController.class);
    private static final String INVALID_SERVICE_ID = "service_id is invalid.";

    @Autowired
    private KeyConfig keyConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PaymentService paymentService;

    @Value("${gov.pay.url}")
    private String url;

    @ApiOperation(value = "Search transaction log", notes = "Search transaction log for supplied search criteria")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Transaction search request succeeded"),
            @ApiResponse(code = 401, message = "Credentials are required to access this resource")
    })
    @RequestMapping(value = "/payments", method = RequestMethod.GET)
    public ResponseEntity<List<TransactionRecord>> searchPayment(@ApiParam(value = "service id") @RequestHeader(value = "service_id") String serviceId,
                                                                 @ApiParam(value = "amount") @RequestParam(value = "amount", required = false) Integer amount,
                                                                 @ApiParam(value = "application reference") @RequestParam(value = "application_reference", required = false) String applicationReference,
                                                                 @ApiParam(value = "description") @RequestParam(value = "description", required = false) String description,
                                                                 @ApiParam(value = "payment reference") @RequestParam(value = "payment_reference", required = false) String paymentReference,
                                                                 @ApiParam(value = "created date") @RequestParam(value = "created_date", required = false) String createdDate,
                                                                 @ApiParam(value = "email") @RequestParam(value = "email", required = false) String email) {
        if (!isValid(serviceId)) {
            return new ResponseEntity(INVALID_SERVICE_ID, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setAmount(amount);
        searchCriteria.setApplicationReference(applicationReference);
        searchCriteria.setDescription(description);
        searchCriteria.setPaymentReference(paymentReference);
        searchCriteria.setServiceId(serviceId);
        searchCriteria.setCreatedDate(createdDate);
        searchCriteria.setEmail(email);
        return new ResponseEntity<>(paymentService.searchPayment(searchCriteria), OK);

    }


    @ApiOperation(value = "Create payment", notes = "Create payment")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Payment has been created"),
            @ApiResponse(code = 400, message = "The server cannot process the request due to a client error, eg missing details in the request."),
            @ApiResponse(code = 401, message = "Required authentication has failed or not been provided"),
            @ApiResponse(code = 422, message = "Invalid or missing attribute")
    })
    @RequestMapping(value = "/payments", method = RequestMethod.POST)
    public ResponseEntity<CreatePaymentResponse> createPayment(@ApiParam(value = "service id") @RequestHeader(value = "service_id") String serviceId,
                                                               @ApiParam(value = "payment request body") @RequestBody(required = true) CreatePaymentRequest payload,
                                                               HttpServletRequest httpServletRequest) {
        payload.setServiceId(serviceId);
        if (!isValid(serviceId)) {
            return new ResponseEntity(INVALID_SERVICE_ID, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (!payload.isValid()) {
            return new ResponseEntity(payload.getValidationMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
        }

        GDSCreatePaymentRequest paymentRequest = new GDSCreatePaymentRequest(payload);
        HttpEntity<GDSCreatePaymentRequest> entity = new HttpEntity<>(paymentRequest, getHeaders(serviceId));
        ResponseEntity<GDSCreatePaymentResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity, GDSCreatePaymentResponse.class);
        String url = httpServletRequest.getRequestURL().toString();
        CreatePaymentResponse createPaymentResponse = new CreatePaymentResponse(response.getBody(), url);
        paymentService.storePayment(payload, response.getBody());

        return new ResponseEntity<>(createPaymentResponse, response.getStatusCode());
    }


    @ApiOperation(value = "Get payment details by id", notes = "Get payment details for supplied payment id")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Payment information request succeeded"),
            @ApiResponse(code = 401, message = "Credentials are required to access this resource"),
            @ApiResponse(code = 404, message = "Payment not found")
    })
    @RequestMapping(value = "/payments/{paymentId}", method = RequestMethod.GET)

    public ResponseEntity<ViewPaymentResponse> viewPayment(@ApiParam(value = "service id") @RequestHeader(value = "service_id") String serviceId,
                                                           @ApiParam(value = "payment id") @PathVariable("paymentId") String paymentId) {
        if (!isValid(serviceId)) {
            return new ResponseEntity(INVALID_SERVICE_ID, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        ResponseEntity<GDSViewPaymentResponse> response = restTemplate.exchange(url + "/" + paymentId, GET, new HttpEntity(getHeaders(serviceId)), GDSViewPaymentResponse.class);
        ViewPaymentResponse viewPaymentResponse = new ViewPaymentResponse(response.getBody());
        ResponseEntity<ViewPaymentResponse> responseEntity = new ResponseEntity<>(viewPaymentResponse, response.getStatusCode());
        paymentService.updatePayment(response.getBody().getPaymentId(), response.getBody().getState().getStatus());
        return responseEntity;
    }

    @ApiOperation(value = "Cancel payment", notes = "Cancel payment for supplied payment id")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Payment cancel request succeeded"),
            @ApiResponse(code = 400, message = "Cancellation of payment failed"),
            @ApiResponse(code = 404, message = "Payment not found")
    })
    @RequestMapping(value = "/payments/{paymentId}/cancel", method = RequestMethod.POST)
    public ResponseEntity<String> cancelPayment(@ApiParam(value = "service id") @RequestHeader(value = "service_id") String serviceId,
                                                @ApiParam(value = "payment id") @PathVariable("paymentId") String paymentId) {
        if (!isValid(serviceId)) {
            return new ResponseEntity(INVALID_SERVICE_ID, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        HttpEntity entity = new HttpEntity(getHeaders(serviceId));
        return restTemplate.exchange(url + "/" + paymentId + "/cancel", HttpMethod.POST, entity, String.class);
    }

    @ExceptionHandler(value = {HttpClientErrorException.class})
    public ResponseEntity httpClientErrorException(HttpClientErrorException e) {
        // TODO: map error codes to relevant exceptions as per https://gds-payments.gelato.io/docs/versions/1.0.0/api-reference
        if (e.getStatusCode() == NOT_FOUND) {
            return new ResponseEntity(NOT_FOUND);
        } else if (e.getStatusCode() == BAD_REQUEST) {
            return new ResponseEntity(BAD_REQUEST);
        } else {
            LOG.error("Unknown error calling GDS service", e);
            return new ResponseEntity(INTERNAL_SERVER_ERROR);
        }
    }

    private HttpHeaders getHeaders(String serviceId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, getAuth(serviceId));
        return headers;
    }

    private String getAuth(String serviceId) {
        return "Bearer " + keyConfig.getKey().get(serviceId);
    }

    private boolean isValid(String serviceId) {
        return keyConfig.getKey().containsKey(serviceId);

    }
}