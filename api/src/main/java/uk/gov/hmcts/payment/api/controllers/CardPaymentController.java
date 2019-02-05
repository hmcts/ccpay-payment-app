package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.*;
import liquibase.util.StringUtils;
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
import uk.gov.hmcts.payment.api.dto.PaymentServiceRequest;
import uk.gov.hmcts.payment.api.dto.PciPalPaymentRequest;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.external.client.dto.CardDetails;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayException;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayPaymentNotFoundException;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.CardDetailsService;
import uk.gov.hmcts.payment.api.service.DelegatingPaymentService;
import uk.gov.hmcts.payment.api.service.PciPalPaymentService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import javax.validation.Valid;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.web.bind.annotation.RequestMethod.GET;


/**
 * Card payment controller
 */

@RestController
@Api(tags = {"Card Payment"})
@SwaggerDefinition(tags = {@Tag(name = "CardPaymentController", description = "Card payment REST API")})
public class CardPaymentController {
    private static final Logger LOG = LoggerFactory.getLogger(CardPaymentController.class);

    private final DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService;
    private final PaymentDtoMapper paymentDtoMapper;
    private final CardDetailsService<CardDetails, String> cardDetailsService;
    private final PciPalPaymentService pciPalPaymentService;

    @Autowired
    public CardPaymentController(DelegatingPaymentService<PaymentFeeLink, String> cardDelegatingPaymentService,
                                 PaymentDtoMapper paymentDtoMapper,
                                 CardDetailsService<CardDetails, String> cardDetailsService, PciPalPaymentService pciPalPaymentService) {
        this.delegatingPaymentService = cardDelegatingPaymentService;
        this.paymentDtoMapper = paymentDtoMapper;
        this.cardDetailsService = cardDetailsService;
        this.pciPalPaymentService = pciPalPaymentService;
    }

    @ApiOperation(value = "Create card payment", notes = "Create card payment")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Payment created"),
        @ApiResponse(code = 400, message = "Payment creation failed"),
        @ApiResponse(code = 422, message = "Invalid or missing attribute")
    })
    @PostMapping(value = "/card-payments")
    @ResponseBody
    public ResponseEntity<PaymentDto> createCardPayment(
        @RequestHeader(value = "return-url") String returnURL,
        @RequestHeader(value = "service-callback-url", required = false) String serviceCallbackUrl,
        @Valid @RequestBody CardPaymentRequest request) throws CheckDigitException {
        String paymentReference = PaymentReference.getInstance().getNext();

        if (StringUtils.isEmpty(request.getChannel()) || StringUtils.isEmpty(request.getProvider())) {
            request.setChannel("online");
            request.setProvider("gov pay");
        }

        PaymentServiceRequest paymentServiceRequest = PaymentServiceRequest.paymentServiceRequestWith()
            .paymentReference(paymentReference)
            .description(request.getDescription())
            .returnUrl(returnURL)
            .ccdCaseNumber(request.getCcdCaseNumber())
            .caseReference(request.getCaseReference())
            .currency(request.getCurrency().getCode())
            .siteId(request.getSiteId())
            .serviceType(request.getService().getName())
            .fees(paymentDtoMapper.toFees(request.getFees()))
            .amount(request.getAmount())
            .serviceCallbackUrl(serviceCallbackUrl)
            .channel(request.getChannel())
            .provider(request.getProvider())
            .build();

        PaymentFeeLink paymentLink = delegatingPaymentService.create(paymentServiceRequest);
        PaymentDto paymentDto = paymentDtoMapper.toCardPaymentDto(paymentLink);
        PciPalPaymentRequest pciPalPaymentRequest = PciPalPaymentRequest.pciPalPaymentRequestWith().orderAmount(request.getAmount().toString()).orderCurrency(request.getCurrency().getCode())
                .orderReference(paymentLink.getPaymentReference()).build();

        if (request.getChannel().equals("telephony")) {
            pciPalPaymentRequest.setCustomData1(paymentLink.getPayments().get(0).getCcdCaseNumber());
            String link = pciPalPaymentService.sendIntialPaymentRequest(pciPalPaymentRequest,request.getService().getName());
            paymentDto = paymentDtoMapper.toPciPalCardPaymentDto(paymentLink,link);
        }

        return new ResponseEntity<>(paymentDto, CREATED);
    }

    @ApiOperation(value = "Get card payment details by payment reference", notes = "Get payment details for supplied payment reference")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payment retrieved"),
        @ApiResponse(code = 403, message = "Payment info forbidden"),
        @ApiResponse(code = 404, message = "Payment not found")
    })
    @GetMapping(value = "/card-payments/{reference}")
    public PaymentDto retrieve(@PathVariable("reference") String paymentReference) {
        return paymentDtoMapper.toRetrieveCardPaymentResponseDto(delegatingPaymentService.retrieve(paymentReference));
    }

    @ApiOperation(value = "Get card payment details with card details by payment reference", notes = "Get payment details with card details for supplied payment reference")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payment card details retrieved"),
        @ApiResponse(code = 404, message = "Payment card details not found")
    })
    @RequestMapping(value = "/card-payments/{reference}/details", method = GET)
    public CardDetails retrieveWithCardDetails(@PathVariable("reference") String paymentReference) {
        return cardDetailsService.retrieve(paymentReference);
    }

    @ApiOperation(value = "Get card payment statuses by payment reference", notes = "Get payment statuses for supplied payment reference")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payment retrieved"),
        @ApiResponse(code = 404, message = "Payment not found")
    })
    @GetMapping(value = "/card-payments/{reference}/statuses")
    public PaymentDto retrievePaymentStatus(@PathVariable("reference") String paymentReference) {
        return paymentDtoMapper.toRetrievePaymentStatusesDto(delegatingPaymentService.retrieve(paymentReference));
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
