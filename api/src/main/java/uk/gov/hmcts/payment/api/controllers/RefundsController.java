package uk.gov.hmcts.payment.api.controllers;


import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import uk.gov.hmcts.payment.api.dto.PaymentRefundRequest;
import uk.gov.hmcts.payment.api.dto.RefundResponse;
import uk.gov.hmcts.payment.api.dto.ResubmitRefundRemissionRequest;
import uk.gov.hmcts.payment.api.exception.InvalidPartialRefundRequestException;
import uk.gov.hmcts.payment.api.dto.RetrospectiveRemissionRequest;
import uk.gov.hmcts.payment.api.exception.InvalidRefundRequestException;
import uk.gov.hmcts.payment.api.service.PaymentRefundsService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.NonPBAPaymentException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotSuccessException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.RemissionNotFoundException;

import jakarta.validation.Valid;

@RestController
@Tag(name = "RefundsController", description = "Refunds REST API")
public class RefundsController {

    private static final Logger LOG = LoggerFactory.getLogger(RefundsController.class);

    @Autowired
    private PaymentRefundsService paymentRefundsService;

    @Operation(summary = "Create refund-for-payment", description = "Create refund payment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Refund created"),
        @ApiResponse(responseCode = "400", description = "Refund creation failed"),
        @ApiResponse(responseCode = "404", description = "Payment reference could not be found"),
        @ApiResponse(responseCode = "504", description = "Unable to process refund information, please try again later"),
        @ApiResponse(responseCode = "422", description = "Invalid or missing attribute")
    })
    @PostMapping(value = "/refund-for-payment")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<RefundResponse> createRefundPayment(@Valid @RequestBody PaymentRefundRequest paymentRefundRequest, @RequestHeader(required = false) MultiValueMap<String, String> headers) {
        return paymentRefundsService.createRefund(paymentRefundRequest, headers);
    }

    @PostMapping(value = "/refund-retro-remission")
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<RefundResponse> createRefundForRetrospective(@Valid @RequestBody RetrospectiveRemissionRequest
                                                                           request, @RequestHeader(required = false) MultiValueMap<String, String> headers) {
        return paymentRefundsService.createAndValidateRetrospectiveRemissionRequest(request, headers);
    }


    @Operation(summary = "Update the remission amount in resubmit journey", description = "Update the remission amount in resubmit journey")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "400", description = "Amount should not be more than Payment amount"),
        @ApiResponse(responseCode = "400", description = "Amount should not be more than remission amount")
    })
    @PatchMapping("/refund/resubmit/{payment-reference}")
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity updateRemissionAmountResubmitRefund(
        @RequestHeader("Authorization") String authorization,
        @PathVariable("payment-reference") String paymentReference,
        @RequestBody @Valid ResubmitRefundRemissionRequest request) {
        LOG.info("Inside updateRemissionAmountResubmitRefund with paymentReference: {}", paymentReference);
        return paymentRefundsService.updateTheRemissionAmount(paymentReference, request);
    }

    @Operation(summary = "Delete refund details by refund reference", description = "Delete Refund details for supplied refund reference")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Refund deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Refund not found for the given reference")
    })
    @DeleteMapping(value = "/refund/{refundReference}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteByRefundReference(@PathVariable("refundReference") String refundReference,
                                        @RequestHeader(required = false) MultiValueMap<String, String> headers) {
        paymentRefundsService.deleteByRefundReference(refundReference, headers);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({PaymentNotSuccessException.class, NonPBAPaymentException.class, RemissionNotFoundException.class, InvalidRefundRequestException.class, InvalidPartialRefundRequestException.class})
    public String return400(Exception ex) {
        LOG.error(ex.getMessage(), ex);
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(PaymentNotFoundException.class)
    public String notFound(PaymentNotFoundException ex) {
        LOG.error(ex.getMessage(), ex);
        return ex.getMessage();
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity returnClientException(HttpClientErrorException ex) {
        LOG.error(ex.getMessage(), ex);
        return new ResponseEntity<>(ex.getResponseBodyAsString(), ex.getStatusCode());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(HttpServerErrorException.class)
    public String returnServerException(HttpServerErrorException ex) {
        LOG.error(ex.getMessage(), ex);
        return ex.getResponseBodyAsString();
    }
}
