package uk.gov.justice.payment.api.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.justice.payment.api.configuration.GovPayConfig;
import uk.gov.justice.payment.api.controllers.dto.CreatePaymentRequestDto;
import uk.gov.justice.payment.api.controllers.dto.PaymentDto;
import uk.gov.justice.payment.api.controllers.dto.PaymentDtoFactory;
import uk.gov.justice.payment.api.external.client.GovPayClient;
import uk.gov.justice.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.justice.payment.api.external.client.dto.Payment;
import uk.gov.justice.payment.api.external.client.exceptions.CancellationFailedException;
import uk.gov.justice.payment.api.external.client.exceptions.GovPayException;
import uk.gov.justice.payment.api.external.client.exceptions.PaymentNotFoundException;
import uk.gov.justice.payment.api.json.api.TransactionRecord;
import uk.gov.justice.payment.api.parameters.serviceid.ServiceId;
import uk.gov.justice.payment.api.services.PaymentService;

import javax.validation.Valid;
import java.util.List;

import static org.springframework.http.HttpStatus.*;
import static uk.gov.justice.payment.api.domain.PaymentDetails.paymentDetailsWith;
import static uk.gov.justice.payment.api.services.SearchCriteria.searchCriteriaWith;


@RestController
@Api(value = "/payment", description = "Payment REST API")
public class PaymentController {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentController.class);

    private final ObjectMapper objectMapper;
    private final GovPayConfig govPayConfig;
    private final GovPayClient govPayClient;
    private final PaymentService paymentService;
    private final PaymentDtoFactory paymentDtoFactory;

    @Autowired
    public PaymentController(ObjectMapper objectMapper, GovPayConfig govPayConfig, GovPayClient govPayClient, PaymentService paymentService, PaymentDtoFactory paymentDtoFactory) {
        this.objectMapper = objectMapper;
        this.govPayConfig = govPayConfig;
        this.govPayClient = govPayClient;
        this.paymentService = paymentService;
        this.paymentDtoFactory = paymentDtoFactory;
    }

    @ApiOperation(value = "Search transaction log", notes = "Search transaction log for supplied search criteria")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Transaction search request succeeded")
    })
    @RequestMapping(value = "/payments", method = RequestMethod.GET)
    public List<TransactionRecord> searchPayment(@ServiceId String serviceId,
                                                 @RequestParam(value = "amount", required = false) Integer amount,
                                                 @RequestParam(value = "application_reference", required = false) String applicationReference,
                                                 @RequestParam(value = "description", required = false) String description,
                                                 @RequestParam(value = "payment_reference", required = false) String paymentReference,
                                                 @RequestParam(value = "created_date", required = false) String createdDate,
                                                 @RequestParam(value = "email", required = false) String email) {

        return paymentService.searchPayment(searchCriteriaWith()
                .amount(amount)
                .applicationReference(applicationReference)
                .description(description)
                .paymentReference(paymentReference)
                .serviceId(serviceId)
                .createdDate(createdDate)
                .email(email)
                .build()
        );
    }

    @ApiOperation(value = "Create payment", notes = "Create payment")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Payment created"),
            @ApiResponse(code = 400, message = "Payment creation failed"),
            @ApiResponse(code = 422, message = "Invalid or missing attribute")
    })
    @RequestMapping(value = "/payments", method = RequestMethod.POST)
    public ResponseEntity<PaymentDto> createPayment(@ServiceId String serviceId,
                                                    @Valid @RequestBody CreatePaymentRequestDto request) throws JsonProcessingException {
        Payment payment = govPayClient.createPayment(
                govPayConfig.getKeyForService(serviceId),
                new CreatePaymentRequest(request.getAmount(), request.getApplicationReference(), request.getDescription(), request.getReturnUrl())
        );

        paymentService.storePayment(paymentDetailsWith()
                .paymentId(payment.getPaymentId())
                .amount(payment.getAmount())
                .paymentReference(request.getPaymentReference())
                .applicationReference(request.getApplicationReference())
                .serviceId(serviceId)
                .description(request.getDescription())
                .returnUrl(request.getReturnUrl())
                .email(request.getEmail())
                .response(objectMapper.writeValueAsString(payment))
                .status(payment.getState().getStatus())
                .createdDate(payment.getCreatedDate())
                .build());

        return new ResponseEntity<>(paymentDtoFactory.toDto(payment), CREATED);
    }


    @ApiOperation(value = "Get payment details by id", notes = "Get payment details for supplied payment id")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Payment retrieved"),
            @ApiResponse(code = 404, message = "Payment not found")
    })
    @RequestMapping(value = "/payments/{paymentId}", method = RequestMethod.GET)
    public PaymentDto viewPayment(@ServiceId String serviceId,
                                  @PathVariable("paymentId") String paymentId) {
        Payment payment = govPayClient.getPayment(govPayConfig.getKeyForService(serviceId), paymentId);
        paymentService.updatePayment(payment.getPaymentId(), payment.getState().getStatus());
        return paymentDtoFactory.toDto(payment);
    }

    @ApiOperation(value = "Cancel payment", notes = "Cancel payment for supplied payment id")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Payment canceled"),
            @ApiResponse(code = 400, message = "Cancellation failed"),
            @ApiResponse(code = 404, message = "Payment not found")
    })
    @RequestMapping(value = "/payments/{paymentId}/cancel", method = RequestMethod.POST)
    public ResponseEntity<?> cancelPayment(@ServiceId String serviceId,
                                           @PathVariable("paymentId") String paymentId) {
        try {
            govPayClient.cancelPayment(govPayConfig.getKeyForService(serviceId), paymentId);
            return new ResponseEntity<>(NO_CONTENT);
        } catch (CancellationFailedException e) {
            LOG.info("Cancellation failed for paymentId: " + paymentId);
            return new ResponseEntity(BAD_REQUEST);
        }
    }

    @ExceptionHandler(value = {PaymentNotFoundException.class})
    public ResponseEntity httpClientErrorException(PaymentNotFoundException e) {
        return new ResponseEntity(NOT_FOUND);
    }

    @ExceptionHandler(value = {GovPayException.class})
    public ResponseEntity httpClientErrorException(GovPayException e) {
        LOG.error("Error while calling payments", e);
        return new ResponseEntity(INTERNAL_SERVER_ERROR);
    }
}