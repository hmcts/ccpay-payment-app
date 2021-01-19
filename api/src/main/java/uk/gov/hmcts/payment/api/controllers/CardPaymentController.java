package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.ff4j.FF4j;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.dto.OrganisationalServiceDto;
import uk.gov.hmcts.payment.api.dto.PaymentServiceRequest;
import uk.gov.hmcts.payment.api.dto.PciPalPaymentRequest;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.external.client.dto.CardDetails;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayCancellationFailedException;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayException;
import uk.gov.hmcts.payment.api.external.client.exceptions.GovPayPaymentNotFoundException;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.CardDetailsService;
import uk.gov.hmcts.payment.api.service.DelegatingPaymentService;
import uk.gov.hmcts.payment.api.service.FeePayApportionService;
import uk.gov.hmcts.payment.api.service.PciPalPaymentService;
import uk.gov.hmcts.payment.api.service.ReferenceDataService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.GatewayTimeoutException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.NoServiceFoundException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import javax.management.ServiceNotFoundException;
import javax.validation.Valid;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
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
    private final FeePayApportionService feePayApportionService;
    private final LaunchDarklyFeatureToggler featureToggler;
    private final ReferenceDataService referenceDataService;

    @Autowired
    private AuthTokenGenerator authTokenGenerator;

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
        this.pciPalPaymentService = pciPalPaymentService;
        this.ff4j = ff4j;
        this.feePayApportionService = feePayApportionService;
        this.featureToggler = featureToggler;
        this.referenceDataService = referenceDataService;
    }

    @ApiOperation(value = "Create card payment", notes = "Create card payment")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Payment created"),
        @ApiResponse(code = 400, message = "Payment creation failed"),
        @ApiResponse(code = 422, message = "Invalid or missing attribute"),
        @ApiResponse(code = 404, message = "No Service Found")
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

        LOG.info("Case Type: {} ",request.getCaseType());
        LOG.info("Service Name : {} ",request.getService().getName());

        if(StringUtils.isNotBlank(request.getCaseType())) {
            getOrganisationalDetails(headers, request);
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

        LOG.info("Language Value : {}", paymentServiceRequest.getLanguage());
        LOG.info("siteId Value : {}", paymentServiceRequest.getSiteId());
        PaymentFeeLink paymentLink = delegatingPaymentService.create(paymentServiceRequest);
        PaymentDto paymentDto = paymentDtoMapper.toCardPaymentDto(paymentLink);

        if (request.getChannel().equals("telephony") && request.getProvider().equals("pci pal")) {
            PciPalPaymentRequest pciPalPaymentRequest = PciPalPaymentRequest.pciPalPaymentRequestWith().orderAmount(request.getAmount().toString()).orderCurrency(request.getCurrency().getCode())
                .orderReference(paymentDto.getReference()).build();
            pciPalPaymentRequest.setCustomData2(paymentLink.getPayments().get(0).getCcdCaseNumber());
            String link = pciPalPaymentService.getPciPalLink(pciPalPaymentRequest, request.getService().name());
            paymentDto = paymentDtoMapper.toPciPalCardPaymentDto(paymentLink, link);
        }
        // trigger Apportion based on the launch darkly feature flag
        boolean apportionFeature = featureToggler.getBooleanValue("apportion-feature", false);
        LOG.info("ApportionFeature Flag Value in CardPaymentController : {}", apportionFeature);
        if (apportionFeature) {
            feePayApportionService.processApportion(paymentLink.getPayments().get(0));
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
        PaymentFeeLink paymentFeeLink = delegatingPaymentService.retrieve(paymentReference);
        Optional<Payment> payment = paymentFeeLink.getPayments().stream()
            .filter(p -> p.getReference().equals(paymentReference)).findAny();
        Payment payment1 = null;
        if (payment.isPresent()) {
            payment1 = payment.get();
        }
        return paymentDtoMapper.toPaymentStatusesDto(payment1);
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


    private void getOrganisationalDetails(MultiValueMap<String, String> headers, CardPaymentRequest request) {
            List<String> serviceAuthTokenPaymentList = new ArrayList<>();

            MultiValueMap<String, String> headerMultiValueMapForOrganisationalDetail = new LinkedMultiValueMap<String, String>();
            try {
                serviceAuthTokenPaymentList.add(authTokenGenerator.generate());
                LOG.info("Service Token : {}", serviceAuthTokenPaymentList);
                headerMultiValueMapForOrganisationalDetail.put("Content-Type", headers.get("content-type"));
                //User token
                headerMultiValueMapForOrganisationalDetail.put("Authorization",Collections.singletonList("Bearer " + headers.get("authorization")));
                //Service token
                headerMultiValueMapForOrganisationalDetail.put("ServiceAuthorization", serviceAuthTokenPaymentList);
                //Http headers
                HttpHeaders httpHeaders = new HttpHeaders(headerMultiValueMapForOrganisationalDetail);
                final HttpEntity<String> entity = new HttpEntity<>(headers);

                OrganisationalServiceDto organisationalServiceDto = referenceDataService.getOrganisationalDetail(request.getCaseType(), entity);
                request.setSiteId(organisationalServiceDto.getServiceCode());
                Service.ORGID.setName(organisationalServiceDto.getServiceDescription());
                request.setService(Service.valueOf("ORGID"));
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                LOG.error("ORG ID Ref error status Code {} ", e.getRawStatusCode());
                if( e.getRawStatusCode() == 404){
                    throw new NoServiceFoundException( "No Service found for given CaseType");
                }
                if(e.getRawStatusCode() == 504){
                    throw new GatewayTimeoutException("Unable to retrieve service information. Please try again later");
                }
            }
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
}
