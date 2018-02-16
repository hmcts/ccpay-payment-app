package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.joda.time.MutableDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.payment.api.contract.CardPaymentDto;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayException;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayPaymentNotFoundException;
import uk.gov.hmcts.payment.api.model.CardPaymentService;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;


@RestController
@Api(value = "CardPaymentController", description = "Card payment REST API")
public class CardPaymentController {
    private static final Logger LOG = LoggerFactory.getLogger(CardPaymentController.class);

    private static final String DEFAULT_CURRENCY = "GBP";

    private final CardPaymentService<PaymentFeeLink, String> cardPaymentService;
    private final CardPaymentDtoMapper cardPaymentDtoMapper;

    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
    private static final SimpleDateFormat outformat = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    public CardPaymentController(@Qualifier("loggingCardPaymentService") CardPaymentService<PaymentFeeLink, String> cardCardPaymentService,
                                 CardPaymentDtoMapper cardPaymentDtoMapper) {
        this.cardPaymentService = cardCardPaymentService;
        this.cardPaymentDtoMapper = cardPaymentDtoMapper;
    }

    @ApiOperation(value = "Create card payment", notes = "Create card payment")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Payment created"),
        @ApiResponse(code = 400, message = "Payment creation failed"),
        @ApiResponse(code = 422, message = "Invalid or missing attribute")
    })
    @RequestMapping(value = "/card-payments", method = POST)
    public ResponseEntity<CardPaymentDto> createCardPayment(@RequestHeader(value = "user-id") String userId,
                                                            @RequestHeader(value = "return-url") String returnURL,
                                                            @Valid @RequestBody CardPaymentRequest request) throws CheckDigitException {
        String paymentReference = PaymentReference.getInstance().getNext();

        int amountInPence = request.getAmount().multiply(new BigDecimal(100)).intValue();

        PaymentFeeLink paymentLink = cardPaymentService.create(amountInPence, paymentReference,
            request.getDescription(), request.getReturnUrl(), request.getCcdCaseNumber(), request.getCaseReference(),
            request.getCurrency().getCode(), request.getSiteId(), request.getServiceName(), cardPaymentDtoMapper.toFees(request.getFee()));

        return new ResponseEntity<>(cardPaymentDtoMapper.toCardPaymentDto(paymentLink), CREATED);
    }

    @ApiOperation(value = "Get card payment details by payment reference", notes = "Get payment details for supplied payment reference")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payment retrieved"),
        @ApiResponse(code = 404, message = "Payment not found")
    })
    @RequestMapping(value = "/card-payments/{reference}", method = GET)
    public CardPaymentDto retrieve(@PathVariable("reference") String paymentReference) {
        return cardPaymentDtoMapper.toRetrieveCardPaymentResponseDto(cardPaymentService.retrieve(paymentReference));
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

    private String getYesterdaysDate() {
        Date now = new Date();
        MutableDateTime mtDtNow = new MutableDateTime(now);
        mtDtNow.addDays(-1);
        return sdf.format(mtDtNow.toDate());
    }

    private String getTodaysDate() {
        return sdf.format(new Date());
    }
}
