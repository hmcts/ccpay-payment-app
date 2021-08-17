package uk.gov.hmcts.payment.api.controllers;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import uk.gov.hmcts.payment.api.dto.PaymentRefundRequest;
import uk.gov.hmcts.payment.api.dto.RefundResponse;
import uk.gov.hmcts.payment.api.dto.RetroSpectiveRemissionRequest;
import uk.gov.hmcts.payment.api.exception.InvalidRefundRequestException;
import uk.gov.hmcts.payment.api.service.PaymentRefundsService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.GatewayTimeoutException;
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
        return paymentRefundsService.CreateRefund(paymentRefundRequest, headers);
    }

    @PostMapping(value = "/refund-retro-remission")
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<RefundResponse> createRefundForRetroSpective(@Valid @RequestBody RetroSpectiveRemissionRequest
                                                                           request, @RequestHeader(required = false) MultiValueMap<String, String> headers) {
        return paymentRefundsService.createAndValidateRetroSpectiveRemissionRequest(request.getRemissionReference(), headers);
    }


    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({PaymentNotSuccessException.class, NonPBAPaymentException.class, RemissionNotFoundException.class, InvalidRefundRequestException.class})
    public String return400(Exception ex) {
        return ex.getMessage();
    }


    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    @ExceptionHandler(GatewayTimeoutException.class)
    public String return504(GatewayTimeoutException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(PaymentNotFoundException.class)
    public String notFound(PaymentNotFoundException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity returnClientException(HttpClientErrorException ex) {
        return new ResponseEntity<>(ex.getMessage(), ex.getStatusCode());
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity returnServerException(HttpServerErrorException ex) {
        return new ResponseEntity<>(ex.getMessage(), ex.getStatusCode());
    }
}
