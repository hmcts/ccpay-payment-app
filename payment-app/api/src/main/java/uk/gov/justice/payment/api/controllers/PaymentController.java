package uk.gov.justice.payment.api.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.justice.payment.api.contract.CreatePaymentRequestDto;
import uk.gov.justice.payment.api.contract.PaymentDto;
import uk.gov.justice.payment.api.contract.RefundPaymentRequestDto;
import uk.gov.justice.payment.api.exceptions.PaymentNotFoundException;
import uk.gov.justice.payment.api.external.client.exceptions.GovPayCancellationFailedException;
import uk.gov.justice.payment.api.external.client.exceptions.GovPayException;
import uk.gov.justice.payment.api.external.client.exceptions.GovPayPaymentNotFoundException;
import uk.gov.justice.payment.api.external.client.exceptions.GovPayRefundAmountMismatch;
import uk.gov.justice.payment.api.model.PaymentDetails;
import uk.gov.justice.payment.api.parameters.serviceid.ServiceId;
import uk.gov.justice.payment.api.services.PaymentService;

import javax.validation.Valid;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static uk.gov.justice.payment.api.services.PaymentSearchCriteria.searchCriteriaWith;


@RestController
@Api(value = "/payment", description = "Payment REST API")
public class PaymentController {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;
    private final PaymentDtoFactory paymentDtoFactory;

    @Autowired
    public PaymentController(PaymentService paymentService, PaymentDtoFactory paymentDtoFactory) {
        this.paymentService = paymentService;
        this.paymentDtoFactory = paymentDtoFactory;
    }

    @ApiOperation(value = "Search transaction log", notes = "Search transaction log for supplied search criteria")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Transaction search request succeeded")
    })
    @RequestMapping(value = "/payments", method = GET)
    public List<PaymentDto> search(@ServiceId String serviceId,
                                   @RequestParam(value = "amount", required = false) Integer amount,
                                   @RequestParam(value = "application_reference", required = false) String applicationReference,
                                   @RequestParam(value = "description", required = false) String description,
                                   @RequestParam(value = "payment_reference", required = false) String paymentReference,
                                   @RequestParam(value = "created_date", required = false) String createdDate,
                                   @RequestParam(value = "email", required = false) String email) {

        List<PaymentDetails> results = paymentService.find(serviceId, searchCriteriaWith()
                .amount(amount)
                .applicationReference(applicationReference)
                .description(description)
                .paymentReference(paymentReference)
                .createdDate(createdDate)
                .email(email)
                .build()
        );

        return results.stream().map(paymentDtoFactory::toDto).collect(toList());
    }

    @ApiOperation(value = "Create payment", notes = "Create payment")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Payment created"),
            @ApiResponse(code = 400, message = "Payment creation failed"),
            @ApiResponse(code = 422, message = "Invalid or missing attribute")
    })
    @RequestMapping(value = "/payments", method = POST)
    public ResponseEntity<PaymentDto> create(@ServiceId String serviceId,
                                             @Valid @RequestBody CreatePaymentRequestDto request) {
        PaymentDetails paymentDetails = paymentService.create(
                serviceId,
                request.getAmount(),
                request.getEmail(),
                request.getApplicationReference(),
                request.getPaymentReference(),
                request.getDescription(),
                request.getReturnUrl()
        );

        return new ResponseEntity<>(paymentDtoFactory.toDto(paymentDetails), CREATED);
    }

    @ApiOperation(value = "Get payment details by id", notes = "Get payment details for supplied payment id")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Payment retrieved"),
            @ApiResponse(code = 404, message = "Payment not found")
    })
    @RequestMapping(value = "/payments/{paymentId}", method = GET)
    public PaymentDto findByPaymentId(@ServiceId String serviceId, @PathVariable("paymentId") String paymentId) {
        return paymentDtoFactory.toDto(paymentService.findByPaymentId(serviceId, paymentId));
    }

    @ApiOperation(value = "Cancel payment", notes = "Cancel payment for supplied payment id")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Payment canceled"),
            @ApiResponse(code = 400, message = "Cancellation failed"),
            @ApiResponse(code = 404, message = "Payment not found")
    })
    @RequestMapping(value = "/payments/{paymentId}/cancel", method = POST)
    public ResponseEntity<?> cancel(@ServiceId String serviceId, @PathVariable("paymentId") String paymentId) {
        try {
            paymentService.cancel(serviceId, paymentId);
            return new ResponseEntity<>(NO_CONTENT);
        } catch (GovPayCancellationFailedException e) {
            LOG.info("Cancellation failed for paymentId: " + paymentId);
            return new ResponseEntity(BAD_REQUEST);
        }
    }

    @ApiOperation(value = "Refund payment", notes = "Refund payment for supplied application reference")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Refund accepted"),
            @ApiResponse(code = 412, message = "Refund amount available mismatch"),
            @ApiResponse(code = 404, message = "Payment not found")
    })
    @RequestMapping(value = "/payments/{applicationReference}//refunds", method = POST)
    public ResponseEntity<?> refund(@ServiceId String serviceId,
                                    @PathVariable("applicationReference") String applicationReference,
                                    @Valid @RequestBody RefundPaymentRequestDto request) {
        PaymentDetails payment = paymentService.findOne(serviceId, searchCriteriaWith().applicationReference(applicationReference).build());

        try {
            paymentService.refund(serviceId, payment.getPaymentId(), request.getAmount(), request.getRefundAmountAvailable());
            return new ResponseEntity<>(CREATED);
        } catch (GovPayRefundAmountMismatch e) {
            LOG.info("Refund amount available mismatch for paymentId: " + payment.getPaymentId());
            return new ResponseEntity(PRECONDITION_FAILED);
        }
    }

    @ExceptionHandler(value = {GovPayPaymentNotFoundException.class, PaymentNotFoundException.class})
    public ResponseEntity httpClientErrorException() {
        return new ResponseEntity(NOT_FOUND);
    }

    @ExceptionHandler(value = {GovPayException.class})
    public ResponseEntity httpClientErrorException(GovPayException e) {
        LOG.error("Error while calling payments", e);
        return new ResponseEntity(INTERNAL_SERVER_ERROR);
    }
}