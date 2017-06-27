package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.api.contract.CreatePaymentRequestDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.RefundPaymentRequestDto;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayCancellationFailedException;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayException;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayPaymentNotFoundException;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayRefundAmountMismatch;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentService;
import uk.gov.hmcts.payment.api.model.exceptions.PaymentNotFoundException;

import static net.logstash.logback.argument.StructuredArguments.keyValue;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;


@RestController
@Api(value = "/payment", description = "Payment REST API")
public class PaymentController {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService<Payment, Integer> paymentService;
    private final PaymentDtoFactory paymentDtoFactory;

    @Autowired
    public PaymentController(@Qualifier("loggingPaymentService") PaymentService<Payment, Integer> paymentService,
                             PaymentDtoFactory paymentDtoFactory) {
        this.paymentService = paymentService;
        this.paymentDtoFactory = paymentDtoFactory;
    }

    @ApiOperation(value = "Create payment", notes = "Create payment")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Payment created"),
            @ApiResponse(code = 400, message = "Payment creation failed"),
            @ApiResponse(code = 422, message = "Invalid or missing attribute")
    })
    @RequestMapping(value = "/users/{userId}/payments", method = POST)
    public ResponseEntity<PaymentDto> create(@PathVariable("userId") String userId,
                                             @Valid @RequestBody CreatePaymentRequestDto request) {
        Payment payment = paymentService.create(
                request.getAmount(),
                request.getReference(),
                request.getDescription(),
                request.getReturnUrl()
        );

        return new ResponseEntity<>(paymentDtoFactory.toDto(payment), CREATED);
    }

    @ApiOperation(value = "Get payment details by id", notes = "Get payment details for supplied payment id")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Payment retrieved"),
            @ApiResponse(code = 404, message = "Payment not found")
    })
    @RequestMapping(value = "/users/{userId}/payments/{paymentId}", method = GET)
    public PaymentDto retrieve(@PathVariable("userId") String userId,
                               @PathVariable("paymentId") Integer paymentId) {
        return paymentDtoFactory.toDto(paymentService.retrieve(paymentId));
    }

    @ApiOperation(value = "Cancel payment", notes = "Cancel payment for supplied payment id")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Payment canceled"),
            @ApiResponse(code = 400, message = "Cancellation failed"),
            @ApiResponse(code = 404, message = "Payment not found")
    })
    @RequestMapping(value = "/users/{userId}/payments/{paymentId}/cancel", method = POST)
    public ResponseEntity<?> cancel(@PathVariable("userId") String userId,
                                    @PathVariable("paymentId") Integer paymentId) {
        try {
            paymentService.cancel(paymentId);
            return new ResponseEntity<>(NO_CONTENT);
        } catch (GovPayCancellationFailedException e) {
            LOG.info("Cancellation failed", keyValue("paymentId", paymentId));
            return new ResponseEntity(BAD_REQUEST);
        }
    }

    @ApiOperation(value = "Refund payment", notes = "Refund payment for supplied application reference")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Refund accepted"),
            @ApiResponse(code = 412, message = "Refund amount available mismatch"),
            @ApiResponse(code = 404, message = "Payment not found")
    })
    @RequestMapping(value = "/users/{userId}/payments/{paymentId}/refunds", method = POST)
    public ResponseEntity<?> refund(@PathVariable("userId") String userId,
                                    @PathVariable("paymentId") Integer paymentId,
                                    @Valid @RequestBody RefundPaymentRequestDto request) {
        try {
            paymentService.refund(paymentId, request.getAmount(), request.getRefundAmountAvailable());
            return new ResponseEntity<>(CREATED);
        } catch (GovPayRefundAmountMismatch e) {
            LOG.info("Refund amount available mismatch", keyValue("paymentId", paymentId));
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
