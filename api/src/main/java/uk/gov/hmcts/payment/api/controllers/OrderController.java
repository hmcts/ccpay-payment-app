package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.apache.http.MethodNotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.contract.CardPaymentRequest;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.domain.OrderDomainDataEntityMapper;
import uk.gov.hmcts.payment.api.domain.OrderDtoDomainMapper;
import uk.gov.hmcts.payment.api.dto.OrderDto;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.PaymentServiceRequest;
import uk.gov.hmcts.payment.api.dto.PciPalPaymentRequest;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentGroupDtoMapper;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.service.*;
import uk.gov.hmcts.payment.api.util.ReferenceUtil;
import uk.gov.hmcts.payment.api.v1.model.exceptions.*;
import uk.gov.hmcts.payment.referencedata.dto.SiteDTO;

import javax.validation.Valid;

@RestController
@Api(tags = {"Order"})
@SwaggerDefinition(tags = {@Tag(name = "OrderController", description = "Order REST API")})
public class OrderController {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentGroupController.class);

    private final PaymentGroupService<PaymentFeeLink, String> paymentGroupService;

    private final PaymentGroupDtoMapper paymentGroupDtoMapper;

    private final DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService;

    private final PaymentDtoMapper paymentDtoMapper;

    private final PciPalPaymentService pciPalPaymentService;

    private final ReferenceUtil referenceUtil;

    private final ReferenceDataService<SiteDTO> referenceDataService;

    private final PaymentProviderRepository paymentProviderRepository;

    private final PaymentFeeRepository paymentFeeRepository;

    private final FeePayApportionService feePayApportionService;

    private final LaunchDarklyFeatureToggler featureToggler;

    private final FeePayApportionRepository feePayApportionRepository;

    private final Payment2Repository payment2Repository;

    @Autowired
    private OrderDtoDomainMapper orderDtoDomainMapper;

    @Autowired
    private OrderDomainDataEntityMapper orderDomainDataEntityMapper;

    @Autowired
    public OrderController(PaymentGroupService paymentGroupService, PaymentGroupDtoMapper paymentGroupDtoMapper,
                                  DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService,
                                  PaymentDtoMapper paymentDtoMapper, PciPalPaymentService pciPalPaymentService,
                                  ReferenceUtil referenceUtil,
                                  ReferenceDataService<SiteDTO> referenceDataService,
                                  PaymentProviderRepository paymentProviderRespository,
                                  PaymentFeeRepository paymentFeeRepository,
                                  FeePayApportionService feePayApportionService,
                                  LaunchDarklyFeatureToggler featureToggler,
                                  FeePayApportionRepository feePayApportionRepository, Payment2Repository payment2Repository){
        this.paymentGroupService = paymentGroupService;
        this.paymentGroupDtoMapper = paymentGroupDtoMapper;
        this.delegatingPaymentService = delegatingPaymentService;
        this.paymentDtoMapper = paymentDtoMapper;
        this.pciPalPaymentService = pciPalPaymentService;
        this.referenceUtil = referenceUtil;
        this.referenceDataService = referenceDataService;
        this.paymentProviderRepository = paymentProviderRespository;
        this.paymentFeeRepository = paymentFeeRepository;
        this.feePayApportionService = feePayApportionService;
        this.featureToggler = featureToggler;
        this.feePayApportionRepository = feePayApportionRepository;
        this.payment2Repository = payment2Repository;
    }

    @ApiOperation(value = "Add Order with Fees", notes = "Add Order with Fees")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Payment group with fee(s) created"),
        @ApiResponse(code = 400, message = "Payment group creation failed")
    })
    @PostMapping(value = "/order")
    public ResponseEntity<PaymentGroupDto> create(@Valid @RequestBody OrderDto orderDto) {

        //String orderReference = PaymentReference.getInstance().getNext();

        PaymentFeeLink paymentFeeLink = orderDomainDataEntityMapper.toEntity(orderDtoDomainMapper.toDomain(orderDto));

//        List<PaymentFee> feeList = orderDto.getFees().stream()
//            .map(orderDtoDomainMapper::toDomain)
//            .collect(Collectors.toList())
//            .stream().map(orderDomainDataEntityMapper::toEntity)
//            .collect(Collectors.toList());
//
//        PaymentFeeLink feeLink = PaymentFeeLink.paymentFeeLinkWith()
//            .paymentReference(paymentGroupReference)
//            .fees(Lists.newArrayList(feeList))
//            .build();
        PaymentFeeLink link = paymentFeeLink;
        paymentFeeLink.getFees().stream().forEach(fee -> fee.setPaymentLink(link));

        paymentFeeLink = paymentGroupService.addNewFeeWithPaymentGroup(paymentFeeLink);

        return new ResponseEntity<>(paymentGroupDtoMapper.toPaymentGroupDto(paymentFeeLink), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Create card payment in Payment Group", notes = "Create card payment in Payment Group")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Payment created"),
        @ApiResponse(code = 400, message = "Payment creation failed"),
        @ApiResponse(code = 422, message = "Invalid or missing attribute")
    })
    @PostMapping(value = "/order/{order-reference}/pba-payment")
    @ResponseBody
    @Transactional
    public ResponseEntity<PaymentDto> createCardPayment(
        @RequestHeader(value = "return-url") String returnURL,
        @RequestHeader(value = "service-callback-url", required = false) String serviceCallbackUrl,
        @PathVariable("payment-group-reference") String paymentGroupReference,
        @Valid @RequestBody CardPaymentRequest request) throws CheckDigitException, MethodNotSupportedException {

        if (StringUtils.isEmpty(request.getChannel()) || StringUtils.isEmpty(request.getProvider())) {
            request.setChannel("online");
            request.setProvider("gov pay");
        }

        PaymentServiceRequest paymentServiceRequest = PaymentServiceRequest.paymentServiceRequestWith()
            .description(request.getDescription())
            .paymentGroupReference(paymentGroupReference)
            .paymentReference(referenceUtil.getNext("RC"))
            .returnUrl(returnURL)
            .ccdCaseNumber(request.getCcdCaseNumber())
            .caseReference(request.getCaseReference())
            .currency(request.getCurrency().getCode())
            .siteId(request.getSiteId())
            .serviceType(request.getService().getName())
            .amount(request.getAmount())
            .serviceCallbackUrl(serviceCallbackUrl)
            .channel(request.getChannel())
            .provider(request.getProvider())
            .build();

        PaymentFeeLink paymentLink = delegatingPaymentService.update(paymentServiceRequest);
        Payment payment = getPayment(paymentLink, paymentServiceRequest.getPaymentReference());
        PaymentDto paymentDto = paymentDtoMapper.toCardPaymentDto(payment, paymentGroupReference);

        if (request.getChannel().equals("telephony") && request.getProvider().equals("pci pal")) {
            PciPalPaymentRequest pciPalPaymentRequest = PciPalPaymentRequest.pciPalPaymentRequestWith().orderAmount(request.getAmount().toString()).orderCurrency(request.getCurrency().getCode())
                .orderReference(paymentDto.getReference()).build();
            pciPalPaymentRequest.setCustomData2(payment.getCcdCaseNumber());
            String link = pciPalPaymentService.getPciPalLink(pciPalPaymentRequest, request.getService().name());
            paymentDto = paymentDtoMapper.toPciPalCardPaymentDto(paymentLink, payment, link);
        }

        // trigger Apportion based on the launch darkly feature flag
        boolean apportionFeature = featureToggler.getBooleanValue("apportion-feature",false);
        LOG.info("ApportionFeature Flag Value in CardPaymentController : {}", apportionFeature);
        if(apportionFeature) {
            feePayApportionService.processApportion(payment);
        }

        return new ResponseEntity<>(paymentDto, HttpStatus.CREATED);
    }

    private Payment getPayment(PaymentFeeLink paymentFeeLink, String paymentReference){
        return paymentFeeLink.getPayments().stream().filter(p -> p.getReference().equals(paymentReference)).findAny()
            .orElseThrow(() -> new PaymentNotFoundException("Payment with reference " + paymentReference + " does not exists."));
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(InvalidPaymentGroupReferenceException.class)
    public String return403(InvalidPaymentGroupReferenceException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PaymentNotFoundException.class)
    public String return400(PaymentNotFoundException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidFeeRequestException.class)
    public String return400(InvalidFeeRequestException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PaymentException.class)
    public String return400(PaymentException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(DuplicatePaymentException.class)
    public String return400DuplicatePaymentException(DuplicatePaymentException ex) {
        return ex.getMessage();
    }
}
