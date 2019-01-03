package uk.gov.hmcts.payment.api.controllers;


import io.swagger.annotations.*;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayException;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayPaymentNotFoundException;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.DelegatingPaymentService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import javax.validation.Valid;
import java.math.BigDecimal;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 *
 * Card payment controller
 *
 * */

@RestController
@Api(tags = {"ProbateCardPaymentController"})
@SwaggerDefinition(tags = {@Tag(name = "ProbateCardPaymentController", description = "Probate Card Payment API for anonymous user")})

public class ProbateCardPaymentController {

    private static final Logger LOG = LoggerFactory.getLogger(ProbateCardPaymentController.class);

    private final DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService;
    private final PaymentDtoMapper paymentDtoMapper;

    @Autowired
    public ProbateCardPaymentController(DelegatingPaymentService<PaymentFeeLink, String> cardDelegatingPaymentService,
                                 PaymentDtoMapper paymentDtoMapper) {
        this.delegatingPaymentService = cardDelegatingPaymentService;
        this.paymentDtoMapper = paymentDtoMapper;
    }


    @ApiOperation(value = "Create porbate card payment", notes = "Create probate card payment")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Probate payment created"),
        @ApiResponse(code = 400, message = "Probate payment creation failed"),
        @ApiResponse(code = 422, message = "Invalid or missing attribute")
    })
    @RequestMapping(value = "/probate-card-payments", method = POST)
    @ResponseBody
    public ResponseEntity<PaymentDto> createCardPayment(
        @RequestHeader(value = "return-url") String returnURL,
        @RequestHeader(value = "service-callback-url", required = false) String serviceCallbackUrl,
        @Valid @RequestBody CardPaymentRequest request) throws CheckDigitException {
        String paymentReference = PaymentReference.getInstance().getNext();

        int amountInPence = request.getAmount().multiply(new BigDecimal(100)).intValue();

        PaymentFeeLink paymentLink = delegatingPaymentService.create(paymentReference, request.getDescription(), returnURL, request.getCcdCaseNumber(), request.getCaseReference(), request.getCurrency().getCode(), request.getSiteId(), request.getService().getName(), paymentDtoMapper.toFees(request.getFees()), amountInPence,
            serviceCallbackUrl);

        return new ResponseEntity<>(paymentDtoMapper.toCardPaymentDto(paymentLink), CREATED);
    }

    @ApiOperation(value = "Get porbate card payment details by payment reference", notes = "Get probate payment details for supplied payment reference")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Probate payment retrieved"),
        @ApiResponse(code = 403, message = "Probate payment info forbidden"),
        @ApiResponse(code = 404, message = "Payment not found")
    })
    @RequestMapping(value = "/probate-card-payments/{reference}", method = GET)
    public PaymentDto retrieve(@PathVariable("reference") String paymentReference) {
        return paymentDtoMapper.toRetrieveCardPaymentResponseDto(delegatingPaymentService.retrieve(paymentReference));
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

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PaymentException.class)
    public String return400(PaymentException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public String return403(AccessDeniedException ex) {
        return ex.getMessage();
    }
}
