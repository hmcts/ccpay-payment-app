package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.ff4j.FF4j;
import org.owasp.encoder.Encode;
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
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayCancellationFailedException;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayException;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayPaymentNotFoundException;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.CardDetailsService;
import uk.gov.hmcts.payment.api.service.DelegatingPaymentService;
import uk.gov.hmcts.payment.api.service.PciPalPaymentService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Collectors;

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
    private final FF4j ff4j;

    @Autowired
    public CardPaymentController(DelegatingPaymentService<PaymentFeeLink, String> cardDelegatingPaymentService,
                                 PaymentDtoMapper paymentDtoMapper,
                                 CardDetailsService<CardDetails, String> cardDetailsService,
                                 PciPalPaymentService pciPalPaymentService,
                                 FF4j ff4j) {
        this.delegatingPaymentService = cardDelegatingPaymentService;
        this.paymentDtoMapper = paymentDtoMapper;
        this.cardDetailsService = cardDetailsService;
        this.pciPalPaymentService = pciPalPaymentService;
        this.ff4j = ff4j;
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
        @Valid @RequestBody CardPaymentRequest request) throws CheckDigitException, URISyntaxException {

        LOG.info("ITHC Security Fixes are deployed!!!");

        if(! new URI(returnURL).getHost().matches(".*([\\-\\.]hmcts.net|[\\-\\.]gov.uk)")) {
            return new ResponseEntity("returnUrl: Must be an internal domain of hmcts.net or gov.uk", BAD_REQUEST);
        }

        String paymentGroupReference = PaymentReference.getInstance().getNext();

        if (StringUtils.isEmpty(request.getChannel()) || StringUtils.isEmpty(request.getProvider())) {
            request.setChannel("online");
            request.setProvider("gov pay");
        }

        if (request.getCcdCaseNumber() != null && request.getFees() != null) {
            request.setFees(request.getFees()
                .stream()
                .map(feeDto -> {
                    if (feeDto.getCcdCaseNumber() == null || feeDto.getCcdCaseNumber().isEmpty()) {
                        feeDto.setCcdCaseNumber(request.getCcdCaseNumber());
                    }
                    return feeDto;
                })
                .collect(Collectors.toList())
            );
        }

        PaymentServiceRequest paymentServiceRequest = PaymentServiceRequest.paymentServiceRequestWith()
            .paymentGroupReference(paymentGroupReference)
            .description(Encode.forHtml(request.getDescription()))
            .returnUrl(returnURL)
            .ccdCaseNumber(request.getCcdCaseNumber())
            .caseReference(request.getCaseReference())
            .currency(request.getCurrency().getCode())
            .siteId(request.getSiteId())
            .serviceType(request.getService().getName())
            .fees((request.getFees() != null) ? paymentDtoMapper.toFees(request.getFees()) : null)
            .amount(request.getAmount())
            .serviceCallbackUrl(serviceCallbackUrl)
            .channel(request.getChannel())
            .provider(request.getProvider())
            .language((StringUtils.isBlank(request.getLanguage()) || request.getLanguage() != null
                && request.getLanguage().equalsIgnoreCase("string")) ? null : StringUtils.lowerCase(request.getLanguage()))
            //change language to lower case before sending to gov pay
            .build();

        LOG.info("Language Value : {}",paymentServiceRequest.getLanguage());
        PaymentFeeLink paymentLink = delegatingPaymentService.create(paymentServiceRequest);
        PaymentDto paymentDto = paymentDtoMapper.toCardPaymentDto(paymentLink);

        if (request.getChannel().equals("telephony") && request.getProvider().equals("pci pal")) {
            PciPalPaymentRequest pciPalPaymentRequest = PciPalPaymentRequest.pciPalPaymentRequestWith().orderAmount(request.getAmount().toString()).orderCurrency(request.getCurrency().getCode())
                .orderReference(paymentDto.getReference()).build();
            pciPalPaymentRequest.setCustomData2(paymentLink.getPayments().get(0).getCcdCaseNumber());
            String link = pciPalPaymentService.getPciPalLink(pciPalPaymentRequest, request.getService().name());
            paymentDto = paymentDtoMapper.toPciPalCardPaymentDto(paymentLink, link);
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

    @ApiOperation(value = "Cancel payment for supplied payment reference", notes = "Cancel payment for supplied payment reference")
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "Cancellation of payment successful"),
        @ApiResponse(code = 400, message = "Cancellation of payment failed"),
        @ApiResponse(code = 401, message = "Credentials are required to access this resource"),
        @ApiResponse(code = 403, message = "Forbidden-Access Denied"),
        @ApiResponse(code = 404, message = "Payment Not found"),
        @ApiResponse(code = 500, message = "Downstream system error")
    })
    @PostMapping(value = "/card-payments/{reference}/cancel")
    public ResponseEntity cancelPayment(@PathVariable("reference") String paymentReference) {
        if (!ff4j.check("payment-cancel")) {
            throw new PaymentException("Payment cancel feature is not available for usage.");
        }
        delegatingPaymentService.cancel(paymentReference);
        return new ResponseEntity(NO_CONTENT);
    }

    @ExceptionHandler(value = {GovPayCancellationFailedException.class})
    public ResponseEntity cancellationFailedException(GovPayCancellationFailedException ex) {
        return new ResponseEntity(BAD_REQUEST);
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
