package uk.gov.hmcts.payment.api.controllers;


import io.swagger.annotations.*;
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
import uk.gov.hmcts.payment.api.dto.RetroSpectiveRemissionRequest;
import uk.gov.hmcts.payment.api.exception.InvalidRefundRequestException;
import uk.gov.hmcts.payment.api.service.PaymentRefundsService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.NonPBAPaymentException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotSuccessException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.RemissionNotFoundException;

import javax.validation.Valid;

@RestController
@Api(tags = {"Refund group"})
@SwaggerDefinition(tags = {@Tag(name = "RefundsController", description = "Refunds REST API")})
public class RefundsController {

    @Autowired
    private PaymentRefundsService paymentRefundsService;

    @ApiOperation(value = "Create refund-for-payment", notes = "Create refund payment")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Refund created"),
        @ApiResponse(code = 400, message = "Refund creation failed"),
        @ApiResponse(code = 404, message = "Payment reference could not be found"),
        @ApiResponse(code = 504, message = "Unable to process refund information, please try again later"),
        @ApiResponse(code = 422, message = "Invalid or missing attribute")
    })
    @PostMapping(value = "/refund-for-payment")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<RefundResponse> createRefundPayment(@Valid @RequestBody PaymentRefundRequest paymentRefundRequest, @RequestHeader(required = false) MultiValueMap<String, String> headers) {
        return paymentRefundsService.createRefund(paymentRefundRequest, headers);
    }

    @PostMapping(value = "/refund-retro-remission")
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<RefundResponse> createRefundForRetroSpective(@Valid @RequestBody RetroSpectiveRemissionRequest
                                                                           request, @RequestHeader(required = false) MultiValueMap<String, String> headers) {
        return paymentRefundsService.createAndValidateRetroSpectiveRemissionRequest(request, headers);
    }


    @ApiOperation(value = "Update the remission amount in resubmit journey", notes = "Update the remission amount in resubmit journey")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Success"),
        @ApiResponse(code = 400, message = "Amount should not be more than Payment amount"),
        @ApiResponse(code = 400, message = "Amount should not be more than remission amount")
    })
    @PatchMapping("/refund/resubmit/{payment-reference}")
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity updateRemissionAmountResubmitRefund(
        @RequestHeader("Authorization") String authorization,
        @PathVariable("payment-reference") String paymentReference,
        @RequestBody @Valid ResubmitRefundRemissionRequest request) {
        return paymentRefundsService.updateTheRemissionAmount(paymentReference, request);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({PaymentNotSuccessException.class, NonPBAPaymentException.class, RemissionNotFoundException.class, InvalidRefundRequestException.class})
    public String return400(Exception ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(PaymentNotFoundException.class)
    public String notFound(PaymentNotFoundException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity returnClientException(HttpClientErrorException ex) {
        return new ResponseEntity<>(ex.getResponseBodyAsString(), ex.getStatusCode());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(HttpServerErrorException.class)
    public String returnServerException(HttpServerErrorException ex) {
        return ex.getResponseBodyAsString();
    }
}
