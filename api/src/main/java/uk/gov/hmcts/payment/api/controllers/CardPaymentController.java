package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.*;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.dto.mapper.CardPaymentDtoMapper;
import uk.gov.hmcts.payment.api.external.client.dto.CardDetails;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayException;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayPaymentNotFoundException;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.reports.PaymentsReportService;
import uk.gov.hmcts.payment.api.service.CardDetailsService;
import uk.gov.hmcts.payment.api.service.CardPaymentService;
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
@Api(tags = {"CardPaymentController"})
@SwaggerDefinition(tags = {@Tag(name = "CardPaymentController", description = "Card Payment API")})
public class CardPaymentController {
    private static final Logger LOG = LoggerFactory.getLogger(CardPaymentController.class);

    private final CardPaymentService<PaymentFeeLink, String> cardPaymentService;
    private final CardPaymentDtoMapper cardPaymentDtoMapper;
    private final PaymentsReportService paymentsReportService;
    private final CardDetailsService<CardDetails, String> cardDetailsService;

    @Autowired
    public CardPaymentController(@Qualifier("loggingCardPaymentService") CardPaymentService<PaymentFeeLink, String> cardCardPaymentService,
                                 CardPaymentDtoMapper cardPaymentDtoMapper,
                                 PaymentsReportService paymentsReportService,
                                 CardDetailsService<CardDetails, String> cardDetailsService) {
        this.cardPaymentService = cardCardPaymentService;
        this.cardPaymentDtoMapper = cardPaymentDtoMapper;
        this.paymentsReportService = paymentsReportService;
        this.cardDetailsService = cardDetailsService;
    }

    @ApiOperation(value = "Create card payment", notes = "Create card payment")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Payment created"),
        @ApiResponse(code = 400, message = "Payment creation failed"),
        @ApiResponse(code = 422, message = "Invalid or missing attribute")
    })
    @RequestMapping(value = "/card-payments", method = POST)
    @ResponseBody
    public ResponseEntity<PaymentDto> createCardPayment(
        @RequestHeader(value = "return-url") String returnURL,
        @Valid @RequestBody CardPaymentRequest request) throws CheckDigitException {
        String paymentReference = PaymentReference.getInstance().getNext();

        int amountInPence = request.getAmount().multiply(new BigDecimal(100)).intValue();

        PaymentFeeLink paymentLink = cardPaymentService.create(amountInPence, paymentReference,
            request.getDescription(), returnURL, request.getCcdCaseNumber(), request.getCaseReference(),
            request.getCurrency().getCode(), request.getSiteId(), request.getService().getName(), cardPaymentDtoMapper.toFees(request.getFees()));

        return new ResponseEntity<>(cardPaymentDtoMapper.toCardPaymentDto(paymentLink), CREATED);
    }

    @ApiOperation(value = "Get card payment details by payment reference", notes = "Get payment details for supplied payment reference")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payment retrieved"),
        @ApiResponse(code = 404, message = "Payment not found")
    })
    @RequestMapping(value = "/card-payments/{reference}", method = GET)
    public PaymentDto retrieve(@PathVariable("reference") String paymentReference) {
        return cardPaymentDtoMapper.toRetrieveCardPaymentResponseDto(cardPaymentService.retrieve(paymentReference));
    }

    @ApiOperation(value = "Get card payment details with card details by payment reference", notes = "Get payment details with card detaisl for supplied payment reference")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payment card details retrieved"),
        @ApiResponse(code = 404, message = "Payment card details not found")
    })
    @RequestMapping(value = "/card-payments/{reference}/card-details", method = GET)
    public CardDetails retrieveWithCardDetails(@PathVariable("reference") String paymentReference) {
        return cardDetailsService.retrieve(paymentReference);
    }

    @ApiOperation(value = "Get card payment statuses by payment reference", notes = "Get payment statuses for supplied payment reference")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payment retrieved"),
        @ApiResponse(code = 404, message = "Payment not found")
    })
    @RequestMapping(value = "/card-payments/{reference}/statuses", method = GET)
    public PaymentDto retrievePaymentStatus(@PathVariable("reference") String paymentReference) {
        return cardPaymentDtoMapper.toRetrievePaymentStatusesDto(cardPaymentService.retrieve(paymentReference));
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
