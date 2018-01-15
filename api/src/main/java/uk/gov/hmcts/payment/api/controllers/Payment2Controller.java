package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

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
import uk.gov.hmcts.payment.api.contract.CreateCardPaymentRequestDto;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayException;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayPaymentNotFoundException;
import uk.gov.hmcts.payment.api.contract.Payment2Dto;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;
import uk.gov.hmcts.payment.api.model.Payment2Service;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.web.bind.annotation.RequestMethod.POST;


@RestController
@Api(value = "Payment2Controller", description = "Payment REST API")
public class Payment2Controller {
    private static final Logger LOG = LoggerFactory.getLogger(Payment2Controller.class);

    private final Payment2Service<PaymentFeeLink, Integer> cardPayment2Service;
    private final Payment2DtoFactory payment2DtoFactory;

    @Autowired
    public Payment2Controller(@Qualifier("loggingCardPaymentService") Payment2Service<PaymentFeeLink, Integer> cardPayment2Service,
                              Payment2DtoFactory payment2DtoFactory) {
        this.cardPayment2Service = cardPayment2Service;
        this.payment2DtoFactory = payment2DtoFactory;
    }


    @ApiOperation(value = "Create card payment", notes = "Create card payment")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Payment created"),
        @ApiResponse(code = 400, message = "Payment creation failed"),
        @ApiResponse(code = 422, message = "Invalid or missing attribute")
    })
    @RequestMapping(value = "/users/{userId}/cardpayments", method = POST)
    public ResponseEntity<Payment2Dto> createCardPayment(@PathVariable("userId") String userId,
                                                         @RequestBody CreateCardPaymentRequestDto request) {
        String paymentReference = PaymentReference.getInstance().getNext();
        PaymentFeeLink paymentLink = cardPayment2Service.create(request.getAmount(), paymentReference,
            request.getDescription(), request.getReturnUrl(), request.getCcdCaseNumber(), request.getCaseReference(),
            request.getCurrency(), request.getSiteId(), payment2DtoFactory.toFees(request.getFeeDtos()));

        return new ResponseEntity<>(payment2DtoFactory.toCardPaymentDto(paymentLink), CREATED);
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
