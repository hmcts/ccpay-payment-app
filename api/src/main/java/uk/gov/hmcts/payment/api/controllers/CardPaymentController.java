package uk.gov.hmcts.payment.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.ff4j.FF4j;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.dto.OrganisationalServiceDto;
import uk.gov.hmcts.payment.api.dto.PaymentServiceRequest;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.external.client.dto.CardDetails;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayCancellationFailedException;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayException;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayPaymentNotFoundException;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.*;
import uk.gov.hmcts.payment.api.service.CardDetailsService;
import uk.gov.hmcts.payment.api.service.DelegatingPaymentService;
import uk.gov.hmcts.payment.api.service.FeePayApportionService;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.service.PciPalPaymentService;
import uk.gov.hmcts.payment.api.service.ReferenceDataService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.GatewayTimeoutException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.NoServiceFoundException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import javax.validation.Valid;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.web.bind.annotation.RequestMethod.GET;


/**
 * Card payment controller
 */

@RestController
@Tag(name = "CardPaymentController", description = "Card payment REST API")
public class CardPaymentController implements ApplicationContextAware {
    private static final Logger LOG = LoggerFactory.getLogger(CardPaymentController.class);

    private final DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService;
    private final PaymentDtoMapper paymentDtoMapper;
    private final CardDetailsService<CardDetails, String> cardDetailsService;
    private final FF4j ff4j;
    private final FeePayApportionService feePayApportionService;
    private final LaunchDarklyFeatureToggler featureToggler;
    private final ReferenceDataService referenceDataService;

    @Autowired
    private PaymentService<PaymentFeeLink, String> paymentService;

    private ApplicationContext applicationContext;

    @Autowired
    public CardPaymentController(DelegatingPaymentService<PaymentFeeLink, String> cardDelegatingPaymentService,
                                 PaymentDtoMapper paymentDtoMapper,
                                 CardDetailsService<CardDetails, String> cardDetailsService,
                                 PciPalPaymentService pciPalPaymentService,
                                 FF4j ff4j,
                                 FeePayApportionService feePayApportionService, LaunchDarklyFeatureToggler featureToggler, ReferenceDataService referenceDataService) {
        this.delegatingPaymentService = cardDelegatingPaymentService;
        this.paymentDtoMapper = paymentDtoMapper;
        this.cardDetailsService = cardDetailsService;
        this.ff4j = ff4j;
        this.feePayApportionService = feePayApportionService;
        this.featureToggler = featureToggler;
        this.referenceDataService = referenceDataService;
    }

    @Operation(summary = "Create card payment", description = "Create card payment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Payment created"),
        @ApiResponse(responseCode = "400", description = "Payment creation failed"),
        @ApiResponse(responseCode = "422", description = "Invalid or missing attribute"),
        @ApiResponse(responseCode = "404", description = "No Service found for given CaseType"),
        @ApiResponse(responseCode = "504", description = "Unable to retrieve service information. Please try again later")
    })
    @PostMapping(value = "/card-payments")
    @ResponseBody
    @Transactional
    public ResponseEntity<PaymentDto> createCardPayment(
        @RequestHeader(value = "return-url") String returnURL,
        @RequestHeader(value = "service-callback-url", required = false) String serviceCallbackUrl,
        @RequestHeader(required = false) MultiValueMap<String, String> headers,
        @Valid @RequestBody CardPaymentRequest request) throws CheckDigitException {
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

        LOG.info("Case Type: {} ", request.getCaseType());

        if (StringUtils.isNotBlank(request.getCaseType())) {
            OrganisationalServiceDto organisationalServiceDto = referenceDataService.getOrganisationalDetail(Optional.ofNullable(request.getCaseType()), Optional.empty(), headers);
            request.setSiteId(organisationalServiceDto.getServiceCode());
            request.setService(organisationalServiceDto.getServiceDescription());
        } else {
            /*
            Following piece of code to be removed once all Services are on-boarded to Enterprise ORG ID
            */
            request.setService((applicationContext.getBean(PaymentServiceImpl.class)).getServiceNameByCode(request.getService()));
        }

        LOG.info("Service Name : {} ", request.getService());

        PaymentServiceRequest paymentServiceRequest = PaymentServiceRequest.paymentServiceRequestWith()
            .paymentGroupReference(paymentGroupReference)
            .description(Encode.forHtml(request.getDescription()))
            .returnUrl(returnURL)
            .ccdCaseNumber(request.getCcdCaseNumber())
            .caseReference(request.getCaseReference())
            .currency(request.getCurrency().getCode())
            .siteId(request.getSiteId())
            .serviceType(request.getService())
            .fees((request.getFees() != null) ? paymentDtoMapper.toFees(request.getFees()) : null)
            .amount(request.getAmount())
            .serviceCallbackUrl(serviceCallbackUrl)
            .channel(request.getChannel())
            .provider(request.getProvider())
            .language((StringUtils.isBlank(request.getLanguage()) || request.getLanguage() != null
                && request.getLanguage().equalsIgnoreCase("string")) ? null : StringUtils.lowerCase(request.getLanguage()))
            //change language to lower case before sending to gov pay
            .build();

        LOG.info("Language Value : {}", paymentServiceRequest.getLanguage());
        LOG.info("siteId Value : {}", paymentServiceRequest.getSiteId());
        PaymentFeeLink paymentLink = delegatingPaymentService.create(paymentServiceRequest);
        PaymentDto paymentDto = paymentDtoMapper.toCardPaymentDto(paymentLink);

        // trigger Apportion based on the launch darkly feature flag
        boolean apportionFeature = featureToggler.getBooleanValue("apportion-feature", false);
        LOG.info("ApportionFeature Flag Value in CardPaymentController : {}", apportionFeature);
        if (apportionFeature) {
            feePayApportionService.processApportion(paymentLink.getPayments().get(0));
        }
        return new ResponseEntity<>(paymentDto, CREATED);
    }

    @Operation(summary = "Get card payment details by payment reference", description = "Get payment details for supplied payment reference")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment retrieved"),
        @ApiResponse(responseCode = "403", description = "Payment info forbidden"),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping(value = "/card-payments/{reference}")
    public PaymentDto retrieve(@PathVariable("reference") String paymentReference) {
        return paymentDtoMapper.toRetrieveCardPaymentResponseDto(delegatingPaymentService.retrieve(paymentReference), paymentReference);
    }

    @Operation(summary = "Get card payment details with card details by payment reference", description = "Get payment details with card details for supplied payment reference")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment card details retrieved"),
        @ApiResponse(responseCode = "404", description = "Payment card details not found")
    })
    @RequestMapping(value = "/card-payments/{reference}/details", method = GET)
    public CardDetails retrieveWithCardDetails(@PathVariable("reference") String paymentReference) {
        return cardDetailsService.retrieve(paymentReference);
    }

    @Operation(summary = "Get card payment statuses by payment reference", description = "Get payment statuses for supplied payment reference")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment retrieved"),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping(value = "/card-payments/{reference}/statuses")
    public PaymentDto retrievePaymentStatus(@PathVariable("reference") String paymentReference) {
        PaymentFeeLink paymentFeeLink = delegatingPaymentService.retrieve(paymentReference);
        Optional<Payment> payment = paymentFeeLink.getPayments().stream()
            .filter(p -> p.getReference().equals(paymentReference)).findAny();
        Payment payment1 = null;
        if (payment.isPresent()) {
            payment1 = payment.get();
        }
        return paymentDtoMapper.toPaymentStatusesDto(payment1);
    }

    @Operation(summary = "Cancel payment for supplied payment reference", description = "Cancel payment for supplied payment reference")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Cancellation of payment successful"),
        @ApiResponse(responseCode = "400", description = "Cancellation of payment failed"),
        @ApiResponse(responseCode = "401", description = "Credentials are required to access this resource"),
        @ApiResponse(responseCode = "403", description = "Forbidden-Access Denied"),
        @ApiResponse(responseCode = "404", description = "Payment Not found"),
        @ApiResponse(responseCode = "500", description = "Downstream system error")
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

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(value = {NoServiceFoundException.class})
    public String return404(NoServiceFoundException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PaymentException.class)
    public String return400(PaymentException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    @ExceptionHandler(GatewayTimeoutException.class)
    public String return504(GatewayTimeoutException ex) {
        return ex.getMessage();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
