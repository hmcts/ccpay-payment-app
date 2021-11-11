package uk.gov.hmcts.payment.api.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IMessageReceiver;
import com.microsoft.azure.servicebus.TopicClient;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import io.swagger.annotations.*;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestPaymentBo;
import uk.gov.hmcts.payment.api.domain.service.IdempotencyService;
import uk.gov.hmcts.payment.api.domain.service.ServiceRequestDomainService;
import uk.gov.hmcts.payment.api.dto.OnlineCardPaymentRequest;
import uk.gov.hmcts.payment.api.dto.OnlineCardPaymentResponse;
import uk.gov.hmcts.payment.api.dto.ServiceRequestResponseDto;
import uk.gov.hmcts.payment.api.dto.mapper.CreditAccountDtoMapper;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestPaymentDto;
import uk.gov.hmcts.payment.api.exception.LiberataServiceTimeoutException;
import uk.gov.hmcts.payment.api.exceptions.PaymentServiceNotFoundException;
import uk.gov.hmcts.payment.api.model.FeePayApportion;
import uk.gov.hmcts.payment.api.model.IdempotencyKeys;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.DelegatingPaymentService;
import uk.gov.hmcts.payment.api.service.FeePayApportionService;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.service.FeesService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotSuccessException;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @Autowired
    private PaymentService<PaymentFeeLink, String> paymentService;

    @Autowired
    private PaymentDtoMapper paymentDtoMapper;

    @Autowired
    private DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService;

    @Autowired
    private FeePayApportionService feePayApportionService;

    @Autowired
    private FeesService feeService;



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
    public ResponseEntity<ServiceRequestResponseDto> create(@Valid @RequestBody ServiceRequestDto serviceRequestDto, @RequestHeader(required = false) MultiValueMap<String, String> headers){

        ResponseEntity<ServiceRequestResponseDto> serviceRequestResponseDto = new ResponseEntity<>(serviceRequestDomainService.
            create(serviceRequestDto, headers), HttpStatus.CREATED);

        serviceRequestDomainService.sendMessageTopicCPO(serviceRequestDto,null);

        return serviceRequestResponseDto;
    }

    @ApiOperation(value = "Create credit account payment", notes = "Create credit account payment")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Payment created"),
        @ApiResponse(code = 400, message = "Payment creation failed"),
        @ApiResponse(code = 403, message = "Payment failed due to insufficient funds or the account being on hold"),
        @ApiResponse(code = 404, message = "Account information could not be found"),
        @ApiResponse(code = 504, message = "Unable to retrieve account information, please try again later"),
        @ApiResponse(code = 422, message = "Invalid or missing attribute"),
        @ApiResponse(code = 412, message = "The serviceRequest has already been paid"),
        @ApiResponse(code = 417, message = "The amount should be equal to serviceRequest balance")
    })
    @PostMapping(value = "/service-request/{service-request-reference}/pba-payments")
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ResponseEntity<ServiceRequestPaymentBo> createCreditAccountPaymentForServiceRequest(@RequestHeader(value = "idempotency_key") String idempotencyKey,
                                                                                               @PathVariable("service-request-reference") String serviceRequestReference,
                                                                                               @Valid @RequestBody ServiceRequestPaymentDto serviceRequestPaymentDto) throws CheckDigitException, JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        Function<String, Optional<IdempotencyKeys>> getIdempotencyKey = idempotencyKeyToCheck -> idempotencyService.findTheRecordByIdempotencyKey(idempotencyKeyToCheck);

        Function<IdempotencyKeys, ResponseEntity<?>> validateHashcodeForRequest = idempotencyKeys -> {

            ServiceRequestPaymentBo responseBO;
            try {
                if (!idempotencyKeys.getRequest_hashcode().equals(serviceRequestPaymentDto.hashCodeWithServiceRequestReference(serviceRequestReference))) {
                    return new ResponseEntity<>("Payment already present for idempotency key with different payment details", HttpStatus.CONFLICT); // 409 if hashcode not matched
                }
                if (idempotencyKeys.getResponseCode() >= 500) {
                    return new ResponseEntity<>(idempotencyKeys.getResponseBody(), HttpStatus.valueOf(idempotencyKeys.getResponseCode()));
                }
                responseBO = objectMapper.readValue(idempotencyKeys.getResponseBody(), ServiceRequestPaymentBo.class);
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

        //business validations for serviceRequest
        PaymentFeeLink serviceRequest = serviceRequestDomainService.businessValidationForServiceRequests(serviceRequestDomainService.find(serviceRequestReference), serviceRequestPaymentDto);

        //PBA Payment
        ServiceRequestPaymentBo serviceRequestPaymentBo = null;
        ResponseEntity responseEntity;
        String responseJson;
        try {
            serviceRequestPaymentBo = serviceRequestDomainService.addPayments(serviceRequest, serviceRequestPaymentDto);
            HttpStatus httpStatus;
            if(serviceRequestPaymentBo.getError() != null && serviceRequestPaymentBo.getError().getErrorCode().equals("CA-E0004")) {
                httpStatus = HttpStatus.GONE; //410 for deleted pba accounts
            }else if(serviceRequestPaymentBo.getError() != null && serviceRequestPaymentBo.getError().getErrorCode().equals("CA-E0003")){
                httpStatus = HttpStatus.PRECONDITION_FAILED; //412 for pba account on hold
            }else if(serviceRequestPaymentBo.getError() != null && serviceRequestPaymentBo.getError().getErrorCode().equals("CA-E0001")){
                httpStatus = HttpStatus.PAYMENT_REQUIRED; //402 for pba insufficient funds
            }else{
                httpStatus = HttpStatus.CREATED;
            }

            responseEntity = new ResponseEntity<>(serviceRequestPaymentBo, httpStatus);
            responseJson = objectMapper.writeValueAsString(serviceRequestPaymentBo);
        } catch (LiberataServiceTimeoutException liberataServiceTimeoutException) {
            responseEntity = new ResponseEntity<>(liberataServiceTimeoutException.getMessage(), HttpStatus.GATEWAY_TIMEOUT);
            responseJson = liberataServiceTimeoutException.getMessage();
        }

        //Create Idempotency Record
        return serviceRequestDomainService.createIdempotencyRecord(objectMapper, idempotencyKey, serviceRequestReference, responseJson, responseEntity, serviceRequestPaymentDto);
    }

    private TopicClient topicClient;
    @ApiOperation(value = "Process Dead Letter Queue Messages", notes = "Receive the dead letter queue message from topic and re-process")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Dead letter queue processed")
    })
    @PatchMapping(value = "/jobs/dead-letter-queue-process")
    @Transactional
    public void receiveDeadLetterQueueMessage() throws ServiceBusException, InterruptedException, IOException {

        IMessageReceiver subscriptionClient = serviceRequestDomainService.createDLQConnection();
        serviceRequestDomainService.deadLetterprocess(subscriptionClient);
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
        @ApiResponse(code = 452, message = "The servicerequest has already been paid.\nThe payment amount should be equal to service request balance"),
        @ApiResponse(code = 500, message = "Internal server error"),
        @ApiResponse(code = 504, message = "Unable to connect to online card payment provider, please try again later"),
    })
    @PostMapping(value = "/service-request/{service-request-reference}/card-payments")
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ResponseEntity<OnlineCardPaymentResponse> createCardPayment(@RequestHeader(value = "return-url") String returnURL,
                                                                       @RequestHeader(value = "service-callback-url", required = false) String serviceCallbackURL,
                                                                       @PathVariable("service-request-reference") String serviceRequestReference,
                                                                       @Valid @RequestBody OnlineCardPaymentRequest onlineCardPaymentRequest) throws CheckDigitException, JsonProcessingException {

        return new ResponseEntity<>(serviceRequestDomainService.create(onlineCardPaymentRequest, serviceRequestReference, returnURL, serviceCallbackURL), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Get card payment status by Internal Reference", notes = "Get payment status for supplied Internal Reference")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payment status retrieved"),
        @ApiResponse(code = 404, message = "Internal reference not found")
    })
    @PaymentExternalAPI
    @GetMapping(value = "/card-payments/{internal-reference}/status")
    public PaymentDto retrieveStatusByInternalReference(@PathVariable("internal-reference") String internalReference) {
        Payment payment = paymentService.findPayment(internalReference);
        List<FeePayApportion> feePayApportionList = paymentService.findByPaymentId(payment.getId());
        if(feePayApportionList.isEmpty()){
            throw new PaymentNotSuccessException("Payment is not successful");
        }
        List<PaymentFee> fees = feePayApportionList.stream().map(feePayApportion ->feeService.getPaymentFee(feePayApportion.getFeeId()).get())
            .collect(Collectors.toSet()).stream().collect(Collectors.toList());
        PaymentFeeLink paymentFeeLink = fees.get(0).getPaymentLink();
        LOG.info(" paymentFeeLink getEnterpriseServiceName {}",paymentFeeLink.getEnterpriseServiceName());
        LOG.info(" paymentFeeLink getCcdCaseNumber {}",paymentFeeLink.getCcdCaseNumber());
        return paymentDtoMapper.toRetrieveCardPaymentResponseDtoWithoutExtReference(delegatingPaymentService.retrieve(paymentFeeLink, payment.getReference()));
    }


    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PaymentNotSuccessException.class)
    public String paymentNotSuccess(PaymentNotSuccessException ex) {
        return ex.getMessage();
    }


    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PaymentServiceNotFoundException.class)
    public String paymentNotSuccess(PaymentServiceNotFoundException ex) {
        return ex.getMessage();
    }

}
