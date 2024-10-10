package uk.gov.hmcts.payment.api.v1.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import uk.gov.hmcts.payment.api.v1.contract.CreatePaymentRequestDto;
import uk.gov.hmcts.payment.api.v1.contract.PaymentOldDto;
import uk.gov.hmcts.payment.api.v1.contract.RefundPaymentRequestDto;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayCancellationFailedException;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayException;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayPaymentNotFoundException;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayRefundAmountMismatch;
import uk.gov.hmcts.payment.api.v1.model.PaymentOld;
import uk.gov.hmcts.payment.api.v1.model.PaymentService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import static net.logstash.logback.argument.StructuredArguments.keyValue;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;


@RestController
@Tag(name = "/payment", description = "PaymentOld REST API")
public class PaymentOldController {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentOldController.class);

    private final PaymentService<PaymentOld, Integer> paymentService;
    private final PaymentDtoFactory paymentDtoFactory;

    @Autowired
    public PaymentOldController(@Qualifier("loggingOldPaymentService") PaymentService<PaymentOld, Integer> paymentService,
                                PaymentDtoFactory paymentDtoFactory) {
        this.paymentService = paymentService;
        this.paymentDtoFactory = paymentDtoFactory;
    }

    @Operation(summary = "Create payment", description = "Create payment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "PaymentOld created"),
            @ApiResponse(responseCode = "400", description = "PaymentOld creation failed"),
            @ApiResponse(responseCode = "422", description = "Invalid or missing attribute")
    })
    @RequestMapping(value = "/users/{userId}/payments", method = POST)
    public ResponseEntity<PaymentOldDto> create(@PathVariable("userId") String userId,
                                                @Valid @RequestBody CreatePaymentRequestDto request) {
        PaymentOld paymentOld = paymentService.create(
                request.getAmount(),
                request.getReference(),
                request.getDescription(),
                request.getReturnUrl(),null
        );

        return new ResponseEntity<>(paymentDtoFactory.toDto(paymentOld), CREATED);
    }

    @Operation(summary = "Get payment details by id", description = "Get payment details for supplied payment id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PaymentOld retrieved"),
            @ApiResponse(responseCode = "404", description = "PaymentOld not found")
    })
    @RequestMapping(value = "/users/{userId}/payments/{paymentId}", method = GET)
    public PaymentOldDto retrieve(@PathVariable("userId") String userId,
                                  @PathVariable("paymentId") Integer paymentId) {
        return paymentDtoFactory.toDto(paymentService.retrieve(paymentId));
    }

    @Operation(summary = "Cancel payment", description = "Cancel payment for supplied payment id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "PaymentOld canceled"),
            @ApiResponse(responseCode = "400", description = "Cancellation failed"),
            @ApiResponse(responseCode = "404", description = "PaymentOld not found")
    })
    @RequestMapping(value = "/users/{userId}/payments/{paymentId}/cancel", method = POST)
    public ResponseEntity<?> cancel(@PathVariable("userId") String userId,
                                    @PathVariable("paymentId") Integer paymentId) {
        try {
            paymentService.cancel(paymentId);
            return new ResponseEntity<>(NO_CONTENT);
        } catch (GovPayCancellationFailedException e) {
            LOG.info("Cancellation failed", keyValue("paymentId", paymentId));
            return new ResponseEntity<>(BAD_REQUEST);
        }
    }

    @Operation(summary = "Refund payment", description = "Refund payment for supplied application reference")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Refund accepted"),
            @ApiResponse(responseCode = "412", description = "Refund amount available mismatch"),
            @ApiResponse(responseCode = "404", description = "PaymentOld not found")
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
            return new ResponseEntity<>(PRECONDITION_FAILED);
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
