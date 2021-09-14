package uk.gov.hmcts.payment.api.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.api.domain.model.OrderPaymentBo;
import uk.gov.hmcts.payment.api.domain.service.IdempotencyService;
import uk.gov.hmcts.payment.api.domain.service.ServiceRequestDomainService;
import uk.gov.hmcts.payment.api.dto.OnlineCardPaymentRequest;
import uk.gov.hmcts.payment.api.dto.OnlineCardPaymentResponse;
import uk.gov.hmcts.payment.api.dto.ServiceRequestResponseDto;
import uk.gov.hmcts.payment.api.dto.mapper.CreditAccountDtoMapper;
import uk.gov.hmcts.payment.api.dto.order.OrderPaymentDto;
import uk.gov.hmcts.payment.api.dto.order.ServiceRequestDto;
import uk.gov.hmcts.payment.api.exception.LiberataServiceTimeoutException;
import uk.gov.hmcts.payment.api.model.IdempotencyKeys;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

import javax.validation.Valid;
import java.util.Optional;
import java.util.function.Function;

@RestController
@Api(tags = {"service-request"})
@SwaggerDefinition(tags = {@Tag(name = "ServiceRequestController", description = "Service Request REST API")})
public class ServiceRequestController {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceRequestController.class);
    private static final String FAILED = "failed";

    @Autowired
    private ServiceRequestDomainService serviceRequestDomainService;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private CreditAccountDtoMapper creditAccountDtoMapper;

    @ApiOperation(value = "Create Service Request", notes = "Create Service Request")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Service Request Created"),
        @ApiResponse(code = 400, message = "Service Request Creation Failed"),
        @ApiResponse(code = 401, message = "Credentials are required to access this resource"),
        @ApiResponse(code = 403, message = "Forbidden-Access Denied"),
        @ApiResponse(code = 422, message = "Invalid or missing attribute"),
        @ApiResponse(code = 404, message = "No Service found for given CaseType"),
        @ApiResponse(code = 504, message = "Unable to retrieve service information. Please try again later"),
        @ApiResponse(code = 500, message = "Internal Server")
    })
    @PostMapping(value = "/service-request")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ServiceRequestResponseDto> create(@Valid @RequestBody ServiceRequestDto serviceRequestDto, @RequestHeader(required = false) MultiValueMap<String, String> headers) {
        return new ResponseEntity<>(serviceRequestDomainService.create(serviceRequestDto, headers), HttpStatus.CREATED);
    }


    @ApiOperation(value = "Create credit account payment", notes = "Create credit account payment")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Payment created"),
        @ApiResponse(code = 400, message = "Payment creation failed"),
        @ApiResponse(code = 403, message = "Payment failed due to insufficient funds or the account being on hold"),
        @ApiResponse(code = 404, message = "Account information could not be found"),
        @ApiResponse(code = 504, message = "Unable to retrieve account information, please try again later"),
        @ApiResponse(code = 422, message = "Invalid or missing attribute"),
        @ApiResponse(code = 412, message = "The order has already been paid"),
        @ApiResponse(code = 417, message = "The order amount should be equal to order balance")
    })
    @PostMapping(value = "/order/{order-reference}/credit-account-payment")
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ResponseEntity<OrderPaymentBo> createCreditAccountPayment(@RequestHeader(value = "idempotency_key") String idempotencyKey,
                                                                     @PathVariable("order-reference") String orderReference,
                                                                     @Valid @RequestBody OrderPaymentDto orderPaymentDto) throws CheckDigitException, JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        Function<String, Optional<IdempotencyKeys>> getIdempotencyKey = idempotencyKeyToCheck -> idempotencyService.findTheRecordByIdempotencyKey(idempotencyKeyToCheck);

        Function<IdempotencyKeys, ResponseEntity<?>> validateHashcodeForRequest = idempotencyKeys -> {

            OrderPaymentBo responseBO;
            try {
                if (!idempotencyKeys.getRequest_hashcode().equals(orderPaymentDto.hashCodeWithOrderReference(orderReference))) {
                    return new ResponseEntity<>("Payment already present for idempotency key with different payment details", HttpStatus.CONFLICT); // 409 if hashcode not matched
                }
                if (idempotencyKeys.getResponseCode() >= 500) {
                    return new ResponseEntity<>(idempotencyKeys.getResponseBody(), HttpStatus.valueOf(idempotencyKeys.getResponseCode()));
                }
                responseBO = objectMapper.readValue(idempotencyKeys.getResponseBody(), OrderPaymentBo.class);
            } catch (JsonProcessingException e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return new ResponseEntity<>(responseBO, HttpStatus.valueOf(idempotencyKeys.getResponseCode())); // if hashcode matched
        };

        //Idempotency Check
        Optional<IdempotencyKeys> idempotencyKeysRow = getIdempotencyKey.apply(idempotencyKey);
        if (idempotencyKeysRow.isPresent()) {
            ResponseEntity responseEntity = validateHashcodeForRequest.apply(idempotencyKeysRow.get());
            return responseEntity;
        }

        //business validations for order
        PaymentFeeLink order = serviceRequestDomainService.businessValidationForOrders(serviceRequestDomainService.find(orderReference), orderPaymentDto);

        //PBA Payment
        OrderPaymentBo orderPaymentBo = null;
        ResponseEntity responseEntity;
        String responseJson;
        try {
            orderPaymentBo = serviceRequestDomainService.addPayments(order, orderPaymentDto);
            HttpStatus httpStatus = orderPaymentBo.getStatus().equalsIgnoreCase(FAILED) ? HttpStatus.PAYMENT_REQUIRED : HttpStatus.CREATED; //402 for failed Payment scenarios
            responseEntity = new ResponseEntity<>(orderPaymentBo, httpStatus);
            responseJson = objectMapper.writeValueAsString(orderPaymentBo);
        } catch (LiberataServiceTimeoutException liberataServiceTimeoutException) {
            responseEntity = new ResponseEntity<>(liberataServiceTimeoutException.getMessage(), HttpStatus.GATEWAY_TIMEOUT);
            responseJson = liberataServiceTimeoutException.getMessage();
        }

        //Create Idempotency Record
        return serviceRequestDomainService.createIdempotencyRecord(objectMapper, idempotencyKey, orderReference, responseJson, responseEntity, orderPaymentDto);
    }


    @ApiOperation(value = "Create online card payment", notes = "Create online card payment")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Payment created"),
        @ApiResponse(code = 400, message = "Bad request. Payment creation failed"),
        @ApiResponse(code = 403, message = "Unauthenticated request"),
        @ApiResponse(code = 404, message = "Service request not found"),
        @ApiResponse(code = 409, message = "Idempotency key already exist with different payment details"),
        @ApiResponse(code = 412, message = "The order has already been paid"),
        @ApiResponse(code = 422, message = "Invalid or missing attributes"),
        @ApiResponse(code = 425, message = "Too many requests.\n There is already a payment request is in process for this service request."),
        @ApiResponse(code = 452, message = "The service request has already been paid.\nThe payment amount should be equal to service request balance"),
        @ApiResponse(code = 500, message = "Internal server error"),
        @ApiResponse(code = 504, message = "Unable to connect to online card payment provider, please try again later"),
    })
    @PostMapping(value = "/service-request/{service-request-reference}/card-payments")
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ResponseEntity<OnlineCardPaymentResponse> createCardPayment(@RequestHeader(value = "return-url") String returnURL,
                                                                       @RequestHeader(value = "service-callback-url") String serviceCallbackURL,
                                                                       @PathVariable("service-request-reference") String serviceRequestReference,
                                                                       @Valid @RequestBody OnlineCardPaymentRequest onlineCardPaymentRequest) throws CheckDigitException, JsonProcessingException {

        return new ResponseEntity<>(serviceRequestDomainService.create(onlineCardPaymentRequest, serviceRequestReference, returnURL, serviceCallbackURL), HttpStatus.CREATED);
    }

}
