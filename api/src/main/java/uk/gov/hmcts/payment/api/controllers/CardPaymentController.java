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
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayException;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayPaymentNotFoundException;
import uk.gov.hmcts.payment.api.contract.CardPaymentDto;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;
import uk.gov.hmcts.payment.api.model.CardPaymentService;

import javax.validation.Valid;

import java.math.BigDecimal;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.web.bind.annotation.RequestMethod.POST;


@RestController
@Api(value = "CardPaymentController", description = "Card payment REST API")
public class CardPaymentController {
    private static final Logger LOG = LoggerFactory.getLogger(CardPaymentController.class);

    private final CardPaymentService<PaymentFeeLink, Integer> cardCardPaymentService;
    private final CardPaymentDtoMapper cardPaymentDtoMapper;

    @Autowired
    public CardPaymentController(@Qualifier("loggingCardPaymentService") CardPaymentService<PaymentFeeLink, Integer> cardCardPaymentService,
                                 CardPaymentDtoMapper cardPaymentDtoMapper) {
        this.cardCardPaymentService = cardCardPaymentService;
        this.cardPaymentDtoMapper = cardPaymentDtoMapper;
    }


    @ApiOperation(value = "Create card payment", notes = "Create card payment")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Payment created"),
        @ApiResponse(code = 400, message = "Payment creation failed"),
        @ApiResponse(code = 422, message = "Invalid or missing attribute")
    })
    @RequestMapping(value = "/cardpayments", method = POST)
    public ResponseEntity<CardPaymentDto> createCardPayment(@RequestHeader(value = "userId") String userId,
                                                            @Valid @RequestBody CardPaymentRequest request) {
        String paymentReference = PaymentReference.getInstance().getNext();

        int amountInPence = request.getAmount().movePointRight(2).intValue() * 100;
        PaymentFeeLink paymentLink = cardCardPaymentService.create(amountInPence, paymentReference,
            request.getDescription(), request.getReturnUrl(), request.getCcdCaseNumber(), request.getCaseReference(),
            request.getCurrency(), request.getSiteId(), cardPaymentDtoMapper.toFees(request.getFeeDtos()));

        return new ResponseEntity<>(cardPaymentDtoMapper.toCardPaymentDto(paymentLink), CREATED);
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
