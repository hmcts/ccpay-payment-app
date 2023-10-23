package uk.gov.hmcts.payment.api.controllers;

    import com.fasterxml.jackson.core.JsonProcessingException;
    import com.fasterxml.jackson.databind.ObjectMapper;
    import com.fasterxml.jackson.databind.ObjectWriter;
    import com.microsoft.azure.servicebus.IMessageReceiver;
    import com.microsoft.azure.servicebus.TopicClient;
    import com.microsoft.azure.servicebus.primitives.ServiceBusException;
    import io.swagger.v3.oas.annotations.*;
    import io.swagger.v3.oas.annotations.responses.ApiResponse;
    import io.swagger.v3.oas.annotations.responses.ApiResponses;
    import io.swagger.v3.oas.annotations.tags.Tag;
    import lombok.val;
    import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.ResponseEntity;
    import org.springframework.transaction.annotation.Transactional;
    import org.springframework.util.CollectionUtils;
    import org.springframework.util.MultiValueMap;
    import org.springframework.validation.BindingResult;
    import org.springframework.validation.FieldError;
    import org.springframework.web.bind.annotation.*;
    import uk.gov.hmcts.payment.api.contract.PaymentDto;
    import uk.gov.hmcts.payment.api.domain.model.ServiceRequestPaymentBo;
    import uk.gov.hmcts.payment.api.domain.service.IdempotencyService;
    import uk.gov.hmcts.payment.api.domain.service.ServiceRequestDomainService;
    import uk.gov.hmcts.payment.api.dto.OnlineCardPaymentRequest;
    import uk.gov.hmcts.payment.api.dto.OnlineCardPaymentResponse;
    import uk.gov.hmcts.payment.api.dto.PaymentStatusDto;
    import uk.gov.hmcts.payment.api.dto.ServiceRequestResponseDto;
    import uk.gov.hmcts.payment.api.dto.mapper.CreditAccountDtoMapper;
    import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
    import uk.gov.hmcts.payment.api.dto.mapper.PaymentGroupDtoMapper;
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
    import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotSuccessException;

    import javax.validation.Valid;
    import java.io.IOException;
    import java.util.List;
    import java.util.Optional;
    import java.util.concurrent.TimeUnit;
    import java.util.function.Function;
    import java.util.stream.Collectors;

@RestController
@Tag(name = "ServiceRequestController", description = "Service Request REST API")
@SuppressWarnings("all")
public class ServiceRequestController {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceRequestController.class);
    private static final String FAILED = "failed";

    private static final int PAYMENT_RESPONSE_STATUS_MAX_RETRIES = 6;
    private static final int PAYMENT_RESPONSE_STATUS_SECONDS_DELAY = 3;

    private final ServiceRequestDomainService serviceRequestDomainService;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private PaymentGroupDtoMapper paymentGroup;

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

    private String serviceRequestReference;

    @Autowired
    public ServiceRequestController(
        ServiceRequestDomainService serviceRequestDomainService) {
        this.serviceRequestDomainService = serviceRequestDomainService;
    }

    @Operation(summary = "Create Service Request", description = "Create Service Request")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Service Request Created"),
        @ApiResponse(responseCode = "400", description = "Service Request Creation Failed"),
        @ApiResponse(responseCode = "401", description = "Credentials are required to access this resource"),
        @ApiResponse(responseCode = "403", description = "Forbidden-Access Denied"),
        @ApiResponse(responseCode = "422", description = "Invalid or missing attribute"),
        @ApiResponse(responseCode = "404", description = "No Service found for given CaseType"),
        @ApiResponse(responseCode = "504", description = "Unable to retrieve service information. Please try again later"),
        @ApiResponse(responseCode = "500", description = "Internal Server")
    })
    @PostMapping(value = "/service-request")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ServiceRequestResponseDto> create(@Valid @RequestBody ServiceRequestDto serviceRequestDto,
                                                            @RequestHeader(required = false) MultiValueMap<String, String> headers,
                                                            BindingResult result){

        List<FieldError> errors = result.getFieldErrors();
        LOG.info("Validation Errors in Service Request {}", errors);
        if(!CollectionUtils.isEmpty(errors)) {
            for (FieldError error : errors ) {
                LOG.error(error.getDefaultMessage());
            }
        }

        ResponseEntity<ServiceRequestResponseDto> serviceRequestResponseDto = new ResponseEntity<>(serviceRequestDomainService.
            create(serviceRequestDto, headers), HttpStatus.CREATED);

        ServiceRequestResponseDto serviceRequestResponseDtoBody = serviceRequestResponseDto.getBody();

        if(serviceRequestResponseDtoBody!=null)
            serviceRequestReference = serviceRequestResponseDtoBody.getServiceRequestReference();

        serviceRequestDomainService.sendMessageTopicCPO(serviceRequestDto, serviceRequestReference);

        return serviceRequestResponseDto;
    }

    @Operation(summary = "Create credit account payment", description = "Create credit account payment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Payment created"),
        @ApiResponse(responseCode = "400", description = "Payment creation failed"),
        @ApiResponse(responseCode = "403", description = "Payment failed due to insufficient funds or the account being on hold"),
        @ApiResponse(responseCode = "404", description = "Account information could not be found"),
        @ApiResponse(responseCode = "504", description = "Unable to retrieve account information, please try again later"),
        @ApiResponse(responseCode = "422", description = "Invalid or missing attribute"),
        @ApiResponse(responseCode = "412", description = "The serviceRequest has already been paid"),
        @ApiResponse(responseCode = "417", description = "The amount should be equal to serviceRequest balance")
    })
    @PostMapping(value = "/service-request/{service-request-reference}/pba-payments")
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    //@Transactional
    public ResponseEntity<ServiceRequestPaymentBo> createCreditAccountPaymentForServiceRequest(@Parameter(name = "idempotency_key", hidden = true)@RequestHeader(required = false) String idempotencyKey,
                                                                                               @PathVariable("service-request-reference") String serviceRequestReference,
                                                                                               @Valid @RequestBody ServiceRequestPaymentDto serviceRequestPaymentDto) throws CheckDigitException, JsonProcessingException {

        idempotencyKey = serviceRequestPaymentDto.getIdempotencyKey();
        LOG.info("PBA-CID={}, PBA payment started", idempotencyKey);
        val objectMapper = new ObjectMapper();
        val requestHashCode = serviceRequestPaymentDto.hashCodeWithServiceRequestReference(serviceRequestReference);
        val conflictResponse = new ResponseEntity("Payment already present for idempotency key with different payment details", HttpStatus.CONFLICT); // 409 if hashcode not matched

        LOG.info("PBA-CID={}, PBA payment business validations for serviceRequest", idempotencyKey);
        PaymentFeeLink serviceRequest = serviceRequestDomainService.businessValidationForServiceRequests(serviceRequestDomainService.find(serviceRequestReference), serviceRequestPaymentDto);

        // Function to validate idempotencyKey record from the hash.
        Function<IdempotencyKeys, ResponseEntity<?>> validateHashcodeForRequest = idempotencyKeys -> {

            ServiceRequestPaymentBo responseBO;
            try {
                // Return a conflict however it would appear both idempotencyKeys onjects come from the same serviceRequestPaymentDto?
                if (!idempotencyKeys.getRequestHashcode().equals(serviceRequestPaymentDto.hashCodeWithServiceRequestReference(serviceRequestReference))) {
                    return conflictResponse;
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

        // Display serviceRequestStatusDisplay
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String serviceRequestStatusDisplay = ow.writeValueAsString(serviceRequestPaymentDto);
        LOG.info("PBA-CID={}, Passed service Request Status Display PBA payment : {}", idempotencyKey, serviceRequestStatusDisplay);
        LOG.info("PBA-CID={}, PBA payment idempotency validation", idempotencyKey);

        // Check all existing idempotency records if pending or completed, shouldn't be any of either.
        List<IdempotencyKeys> existingPaymentsForThisServiceRequest = idempotencyService.findTheRecordByRequestHashcode(requestHashCode);
        LOG.warn("PBA-CID={}, Found {} idempotency records for existing payments with same hash", idempotencyKey, existingPaymentsForThisServiceRequest.size(), requestHashCode);
        if (!existingPaymentsForThisServiceRequest.isEmpty()) {

            // For "completed" payments, return conflicted reponse ("409").
            if (isAnyOtherCreatedPaymentForThisServiceRequest(existingPaymentsForThisServiceRequest, idempotencyKey)) {
                LOG.warn("PBA-CID={}, Existing Payment for this SR {} has already been completed", idempotencyKey, serviceRequestReference);
                return conflictResponse;
            }

            // Found an existing pending record - which would only happy if a duplicate payment was found.
            // Wait for a short period and check status again.
            LOG.info("PBA-CID={}, Found existing pending idempotency records, delaying return until one is completed", idempotencyKey);
            for (int i=0; i < PAYMENT_RESPONSE_STATUS_MAX_RETRIES; i++) {
                try {
                    TimeUnit.SECONDS.sleep(PAYMENT_RESPONSE_STATUS_SECONDS_DELAY);
                    LOG.info("PBA-CID={}, Loop i={}", idempotencyKey, i);

                    // Update the list
                    existingPaymentsForThisServiceRequest = idempotencyService.findTheRecordByRequestHashcode(requestHashCode);

                    // Check if any have now completed.
                    if (isAnyOtherCreatedPaymentForThisServiceRequest(existingPaymentsForThisServiceRequest, idempotencyKey)) {
                        Optional<IdempotencyKeys> completedRecord = existingPaymentsForThisServiceRequest.stream().filter(a -> a.getResponseStatus().equals(IdempotencyKeys.ResponseStatusType.completed)).findAny();
                        if (completedRecord.isPresent() && completedRecord.get().getResponseStatus().equals(IdempotencyKeys.ResponseStatusType.completed)) {
                            LOG.info("PBA-CID={}, Found a completed payment with idempotency key {}. Returning response.", idempotencyKey, completedRecord.get().getIdempotencyKey());
                            return (ResponseEntity)validateHashcodeForRequest.apply(completedRecord.get());
                        }
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            // If after waiting and the payment still hasn't completed, return a conflict.
            if (!isAnyOtherCreatedPaymentForThisServiceRequest(existingPaymentsForThisServiceRequest, idempotencyKey)) {
                LOG.error("PBA-CID={}, Unable to find a completed payment and pending records still exist. Returning conflict", idempotencyKey);
                return conflictResponse;
            }
        }

        // Create Idempotency Record - hash record does not exist, so create idempotency record.
        serviceRequestDomainService.createIdempotencyRecord(objectMapper, idempotencyKey, serviceRequestReference,
            null, IdempotencyKeys.ResponseStatusType.pending, null, serviceRequestPaymentDto);

        // DTRJ - Temp delay of 5 seconds - allows a new request to be made to replicate a duplicate payment.
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // PBA Payment
        ServiceRequestPaymentBo serviceRequestPaymentBo = null;
        ResponseEntity responseEntity;
        String responseJson;
        try {
            serviceRequestPaymentBo = serviceRequestDomainService.addPayments(serviceRequest, serviceRequestReference, serviceRequestPaymentDto);
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
            LOG.info("PBA-CID={}, PBA payment status: {}", idempotencyKey, httpStatus);
            responseEntity = new ResponseEntity<>(serviceRequestPaymentBo, httpStatus);
            responseJson = objectMapper.writeValueAsString(serviceRequestPaymentBo);
        } catch (LiberataServiceTimeoutException liberataServiceTimeoutException) {
            LOG.error("PBA-CID={}, Exception from Liberata for PBA payment {}", idempotencyKey, liberataServiceTimeoutException);
            responseEntity = new ResponseEntity<>(liberataServiceTimeoutException.getMessage(), HttpStatus.GATEWAY_TIMEOUT);
            responseJson = liberataServiceTimeoutException.getMessage();
        }

        // Update Idempotency Record
        LOG.info("PBA-CID={}, Payment updating idempotency to completed", idempotencyKey);
        return serviceRequestDomainService.createIdempotencyRecord(objectMapper, idempotencyKey, serviceRequestReference,
            responseJson, IdempotencyKeys.ResponseStatusType.completed, responseEntity, serviceRequestPaymentDto);
    }

    // Loop through and check for completed
    private boolean isAnyOtherCreatedPaymentForThisServiceRequest(List<IdempotencyKeys> idempotencyKeys, String idempotencyKey) {
        return idempotencyKeys.stream()
            .filter(a -> a.getResponseStatus().equals(IdempotencyKeys.ResponseStatusType.completed)
                && !a.getIdempotencyKey().equals(idempotencyKey)).findAny().isPresent();
    }

    private TopicClient topicClient;
    @Operation(summary = "Process Dead Letter Queue Messages", description = "Receive the dead letter queue message from topic and re-process")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dead letter queue processed")
    })
    @PatchMapping(value = "/jobs/dead-letter-queue-process")
    @Transactional
    public void receiveDeadLetterQueueMessage() throws ServiceBusException, InterruptedException, IOException {

        IMessageReceiver subscriptionClient = serviceRequestDomainService.createDLQConnection();
        serviceRequestDomainService.deadLetterProcess(subscriptionClient);
    }

    @Operation(summary = "Create online card payment", description = "Create online card payment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Payment created"),
        @ApiResponse(responseCode = "400", description = "Bad request. Payment creation failed"),
        @ApiResponse(responseCode = "403", description = "Unauthenticated request"),
        @ApiResponse(responseCode = "404", description = "Service request not found"),
        @ApiResponse(responseCode = "409", description = "Idempotency key already exist with different payment details"),
        @ApiResponse(responseCode = "412", description = "The order has already been paid"),
        @ApiResponse(responseCode = "422", description = "Invalid or missing attributes"),
        @ApiResponse(responseCode = "425", description = "Too many requests.\n There is already a payment request is in process for this service request."),
        @ApiResponse(responseCode = "452", description = "The servicerequest has already been paid.\nThe payment amount should be equal to service request balance"),
        @ApiResponse(responseCode = "500", description = "Internal server error"),
        @ApiResponse(responseCode = "504", description = "Unable to connect to online card payment provider, please try again later"),
    })
    @PostMapping(value = "/service-request/{service-request-reference}/card-payments")
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional

    public ResponseEntity<OnlineCardPaymentResponse> createCardPayment(@Parameter(name = "return-url", hidden = true) @RequestHeader(required = false) String returnURL,
                                                                       @RequestHeader(value = "service-callback-url", required = false) String serviceCallbackURL,
                                                                       @PathVariable("service-request-reference") String serviceRequestReference,
                                                                       @Valid @RequestBody OnlineCardPaymentRequest onlineCardPaymentRequest) throws CheckDigitException, JsonProcessingException {
        returnURL = onlineCardPaymentRequest.getReturnUrl();
        return new ResponseEntity<>(serviceRequestDomainService.create(onlineCardPaymentRequest, serviceRequestReference, returnURL, serviceCallbackURL), HttpStatus.CREATED);
    }

    @Operation(summary = "Get card payment status by Internal Reference", description = "Get payment status for supplied Internal Reference")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment status retrieved"),
        @ApiResponse(responseCode = "404", description = "Internal reference not found"),
    })
    @PaymentExternalAPI
    @GetMapping(value = "/card-payments/{internal-reference}/status")
    public PaymentDto retrieveStatusByInternalReference(@PathVariable("internal-reference") String internalReference) throws JsonProcessingException {
        LOG.info("Entered /card-payments/{internal-reference}/status using internalReference: {}", internalReference);
        Payment payment = paymentService.findPayment(internalReference);
        LOG.info("internalReference: {} - Payment: {}", internalReference, payment);
        List<FeePayApportion> feePayApportionList = paymentService.findByPaymentId(payment.getId());
        if(feePayApportionList.isEmpty()){
            throw new PaymentNotSuccessException("Payment is not successful");
        }
        List<PaymentFee> fees = feePayApportionList.stream().map(feePayApportion ->feeService.getPaymentFee(feePayApportion.getFeeId()).get())
            .collect(Collectors.toSet()).stream().collect(Collectors.toList());
        PaymentFeeLink paymentFeeLink = fees.get(0).getPaymentLink();
        LOG.info("paymentFeeLink getEnterpriseServiceName {}",paymentFeeLink.getEnterpriseServiceName());
        LOG.info("paymentFeeLink getCcdCaseNumber {}",paymentFeeLink.getCcdCaseNumber());
        PaymentFeeLink  retrieveDelegatingPaymentService = delegatingPaymentService.retrieve(paymentFeeLink, payment.getReference());
        String serviceRequestStatus = paymentGroup.toPaymentGroupDto(retrieveDelegatingPaymentService).getServiceRequestStatus();
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        Payment paymentNew = paymentService.findPayment(internalReference);
        String serviceRequestReference = paymentFeeLink.getPaymentReference();
        LOG.info("Sending payment to Topic with internalReference: {}", paymentNew.getInternalReference());
        PaymentStatusDto paymentStatusDto = paymentDtoMapper.toPaymentStatusDto(serviceRequestReference, "", paymentNew, serviceRequestStatus);
        serviceRequestDomainService.sendMessageToTopic(paymentStatusDto, paymentFeeLink.getCallBackUrl());
        String jsonpaymentStatusDto = ow.writeValueAsString(paymentStatusDto);
        LOG.info("json format paymentStatusDto to Topic {}",jsonpaymentStatusDto);
        LOG.info("callback URL paymentStatusDto to Topic {}",paymentFeeLink.getCallBackUrl());
        return paymentDtoMapper.toRetrieveCardPaymentResponseDtoWithoutExtReference( retrieveDelegatingPaymentService, internalReference);
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
