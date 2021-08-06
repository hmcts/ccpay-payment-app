package uk.gov.hmcts.payment.api.controllers;


import io.swagger.annotations.Api;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.payment.api.dto.RefundReferenceDto;
import uk.gov.hmcts.payment.api.dto.RefundRequest;
import uk.gov.hmcts.payment.api.dto.RetroSpectiveRemissionRequest;
import uk.gov.hmcts.payment.api.service.RefundService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.NonPBAPaymentException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotSuccessException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.RemissionNotFoundException;

import javax.validation.Valid;

/**
 * Controller for interacting with refunds api and refund feature related activities
 */
@RestController
@Api(tags = {"Refund group"})
@SwaggerDefinition(tags = {@Tag(name = "RefundController", description = "Refund rest api")})
public class RefundController {

    @Autowired
    RefundService refundService;

    @PostMapping(value = "/refund-retro-remission")
    public ResponseEntity<RefundReferenceDto> createRefundForRetroSpective(@Valid @RequestBody RetroSpectiveRemissionRequest
                                                                               request, @RequestHeader(required = false) MultiValueMap<String, String> headers) {
        RefundRequest refundRequest = refundService.createAndValidateRetroSpectiveRemissionRequest(request.getRemissionReference());
        return refundService.createRefundRequestForRetroRemission(headers, refundRequest);
    }


    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PaymentNotSuccessException.class)
    public String return400(PaymentNotSuccessException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(NonPBAPaymentException.class)
    public String return400(NonPBAPaymentException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(RemissionNotFoundException.class)
    public String return400(RemissionNotFoundException ex) {
        return ex.getMessage();
    }
}
