package uk.gov.hmcts.payment.api.controllers;

import com.google.common.collect.Lists;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.apache.http.MethodNotSupportedException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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
import uk.gov.hmcts.payment.api.dto.*;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentGroupDtoMapper;

import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.service.*;
import uk.gov.hmcts.payment.api.util.ReferenceUtil;
import uk.gov.hmcts.payment.api.v1.model.exceptions.InvalidFeeRequestException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.InvalidPaymentGroupReferenceException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;
import uk.gov.hmcts.payment.referencedata.dto.SiteDTO;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@Api(tags = {"Payment group"})
@SwaggerDefinition(tags = {@Tag(name = "PaymentGroupController", description = "Payment group REST API")})
public class PaymentGroupController {

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


    @Autowired
    public PaymentGroupController(PaymentGroupService paymentGroupService, PaymentGroupDtoMapper paymentGroupDtoMapper,
                                  DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService,
                                  PaymentDtoMapper paymentDtoMapper, PciPalPaymentService pciPalPaymentService,
                                  ReferenceUtil referenceUtil,
                                  ReferenceDataService<SiteDTO> referenceDataService,
                                  PaymentProviderRepository paymentProviderRespository,
                                  PaymentFeeRepository paymentFeeRepository,
                                  FeePayApportionService feePayApportionService,
                                  LaunchDarklyFeatureToggler featureToggler,
                                  FeePayApportionRepository feePayApportionRepository){
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
    }

    @ApiOperation(value = "Get payments/remissions/fees details by payment group reference", notes = "Get payments/remissions/fees details for supplied payment group reference")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Payment retrieved"),
        @ApiResponse(code = 403, message = "Payment info forbidden"),
        @ApiResponse(code = 404, message = "Payment not found")
    })
    @GetMapping(value = "/payment-groups/{payment-group-reference}")
    public ResponseEntity<PaymentGroupDto> retrievePayment(@PathVariable("payment-group-reference") String paymentGroupReference) {
        PaymentFeeLink paymentFeeLink = paymentGroupService.findByPaymentGroupReference(paymentGroupReference);

        return new ResponseEntity<>(paymentGroupDtoMapper.toPaymentGroupDto(paymentFeeLink), HttpStatus.OK);
    }

    @ApiOperation(value = "Add Payment Group with Fees", notes = "Add Payment Group with Fees")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Payment group with fee(s) created"),
        @ApiResponse(code = 400, message = "Payment group creation failed")
    })
    @PostMapping(value = "/payment-groups")
    public ResponseEntity<PaymentGroupDto> addNewFee(@Valid @RequestBody PaymentGroupDto paymentGroupDto) {

        String paymentGroupReference = PaymentReference.getInstance().getNext();

        paymentGroupDto.getFees().stream().forEach(f -> {
            if (f.getCcdCaseNumber() == null && f.getReference() == null){
                throw new InvalidFeeRequestException("Either ccdCaseNumber or caseReference is required.");
            }
        });

        List<PaymentFee> feeList = paymentGroupDto.getFees().stream()
            .map(paymentGroupDtoMapper::toPaymentFee).collect(Collectors.toList());

        PaymentFeeLink feeLink = PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference(paymentGroupReference)
            .fees(Lists.newArrayList(feeList))
            .build();
        feeList.stream().forEach(fee -> fee.setPaymentLink(feeLink));

        PaymentFeeLink paymentFeeLink = paymentGroupService.addNewFeeWithPaymentGroup(feeLink);

        return new ResponseEntity<>(paymentGroupDtoMapper.toPaymentGroupDto(paymentFeeLink), HttpStatus.CREATED);
    }


    @ApiOperation(value = "Add new Fee(s) to existing Payment Group", notes = "Add new Fee(s) to existing Payment Group")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Fee(s) added to Payment Group"),
        @ApiResponse(code = 400, message = "Payment group creation failed"),
        @ApiResponse(code = 404, message = "Payment Group not found")
    })
    @PutMapping(value = "/payment-groups/{payment-group-reference}")
    public ResponseEntity<PaymentGroupDto> addNewFeetoPaymentGroup(@PathVariable("payment-group-reference") String paymentGroupReference,
                                                                   @Valid @RequestBody PaymentGroupDto paymentGroupDto) {

        paymentGroupDto.getFees().stream().forEach(f -> {
            if (f.getCcdCaseNumber() == null && f.getReference() == null){
                throw new InvalidFeeRequestException("Either ccdCaseNumber or caseReference is required.");
            }
        });

        PaymentFeeLink paymentFeeLink = paymentGroupService.
            addNewFeetoExistingPaymentGroup(paymentGroupDto.getFees().stream()
                .map(paymentGroupDtoMapper::toPaymentFee).collect(Collectors.toList()), paymentGroupReference);

        return new ResponseEntity<>(paymentGroupDtoMapper.toPaymentGroupDto(paymentFeeLink), HttpStatus.OK);
    }

    @ApiOperation(value = "Create card payment in Payment Group", notes = "Create card payment in Payment Group")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Payment created"),
        @ApiResponse(code = 400, message = "Payment creation failed"),
        @ApiResponse(code = 422, message = "Invalid or missing attribute")
    })
    @PostMapping(value = "/payment-groups/{payment-group-reference}/card-payments")
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

    @ApiOperation(value = "Record a Bulk Scan Payment", notes = "Record a Bulk Scan Payment")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Bulk Scan Payment created"),
        @ApiResponse(code = 400, message = "Bulk Scan Payment creation failed"),
        @ApiResponse(code = 422, message = "Invalid or missing attribute")
    })
    @PostMapping(value = "/payment-groups/{payment-group-reference}/bulk-scan-payments")
    @ResponseBody
    @Transactional
    public ResponseEntity<PaymentDto> recordBulkScanPayment(@PathVariable("payment-group-reference") String paymentGroupReference,
                                                @Valid @RequestBody BulkScanPaymentRequest bulkScanPaymentRequest) throws CheckDigitException {

        List<SiteDTO> sites = referenceDataService.getSiteIDs();

        if (sites.stream().noneMatch(o -> o.getSiteID().equals(bulkScanPaymentRequest.getSiteId()))) {
            throw new PaymentException("Invalid siteID: " + bulkScanPaymentRequest.getSiteId());
        }

        PaymentProvider paymentProvider = bulkScanPaymentRequest.getExternalProvider() != null ?
            paymentProviderRepository.findByNameOrThrow(bulkScanPaymentRequest.getExternalProvider())
            : null;

        Payment payment = Payment.paymentWith()
            .reference(referenceUtil.getNext("RC"))
            .amount(bulkScanPaymentRequest.getAmount())
            .caseReference(bulkScanPaymentRequest.getExceptionRecord())
            .ccdCaseNumber(bulkScanPaymentRequest.getCcdCaseNumber())
            .currency(bulkScanPaymentRequest.getCurrency().getCode())
            .paymentProvider(paymentProvider)
            .serviceType(bulkScanPaymentRequest.getService().getName())
            .paymentMethod(PaymentMethod.paymentMethodWith().name(bulkScanPaymentRequest.getPaymentMethod().getType()).build())
            .paymentStatus(bulkScanPaymentRequest.getPaymentStatus())
            .siteId(bulkScanPaymentRequest.getSiteId())
            .giroSlipNo(bulkScanPaymentRequest.getGiroSlipNo())
            .reportedDateOffline(DateTime.parse(bulkScanPaymentRequest.getBankedDate()).withZone(DateTimeZone.UTC).toDate())
            .paymentChannel(bulkScanPaymentRequest.getPaymentChannel())
            .documentControlNumber(bulkScanPaymentRequest.getDocumentControlNumber())
            .payerName(bulkScanPaymentRequest.getPayerName())
            .bankedDate(DateTime.parse(bulkScanPaymentRequest.getBankedDate()).withZone(DateTimeZone.UTC).toDate())
            .build();

        PaymentFeeLink paymentFeeLink = paymentGroupService.addNewPaymenttoExistingPaymentGroup(payment, paymentGroupReference);

        Payment newPayment = getPayment(paymentFeeLink, payment.getReference());

        // trigger Apportion based on the launch darkly feature flag
        boolean apportionFeature = featureToggler.getBooleanValue("apportion-feature",false);
        LOG.info("ApportionFeature Flag Value in CardPaymentController : {}", apportionFeature);
        if(apportionFeature) {
            feePayApportionService.processApportion(newPayment);

            // Update Fee Amount Due as Payment Status received from Bulk Scan Payment as SUCCESS
            if(newPayment.getPaymentStatus().getName().equalsIgnoreCase("success")) {
                LOG.info("Update Fee Amount Due as Payment Status received from Bulk Scan Payment as SUCCESS!!!");
                updateFeeAmountDue(payment);
            }
        }

        return new ResponseEntity<>(paymentDtoMapper.toBulkScanPaymentDto(newPayment, paymentGroupReference), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Record a Bulk Scan Payment with Payment Group", notes = "Record a Bulk Scan Payment with Payment Group")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Bulk Scan Payment with payment group created"),
        @ApiResponse(code = 400, message = "Bulk Scan Payment creation failed"),
        @ApiResponse(code = 422, message = "Invalid or missing attribute")
    })
    @PostMapping(value = "/payment-groups/bulk-scan-payments")
    @ResponseBody
    public ResponseEntity<PaymentDto> recordUnsolicitedBulkScanPayment(@Valid @RequestBody BulkScanPaymentRequest bulkScanPaymentRequest) throws CheckDigitException {

        List<SiteDTO> sites = referenceDataService.getSiteIDs();

        String paymentGroupReference = PaymentReference.getInstance().getNext();

        if (sites.stream().noneMatch(o -> o.getSiteID().equals(bulkScanPaymentRequest.getSiteId()))) {
            throw new PaymentException("Invalid siteID: " + bulkScanPaymentRequest.getSiteId());
        }

        PaymentProvider paymentProvider = bulkScanPaymentRequest.getExternalProvider() != null ?
            paymentProviderRepository.findByNameOrThrow(bulkScanPaymentRequest.getExternalProvider())
            : null;

        Payment payment = Payment.paymentWith()
            .reference(referenceUtil.getNext("RC"))
            .amount(bulkScanPaymentRequest.getAmount())
            .caseReference(bulkScanPaymentRequest.getExceptionRecord())
            .ccdCaseNumber(bulkScanPaymentRequest.getCcdCaseNumber())
            .currency(bulkScanPaymentRequest.getCurrency().getCode())
            .paymentProvider(paymentProvider)
            .serviceType(bulkScanPaymentRequest.getService().getName())
            .paymentMethod(PaymentMethod.paymentMethodWith().name(bulkScanPaymentRequest.getPaymentMethod().getType()).build())
            .siteId(bulkScanPaymentRequest.getSiteId())
            .giroSlipNo(bulkScanPaymentRequest.getGiroSlipNo())
            .reportedDateOffline(DateTime.parse(bulkScanPaymentRequest.getBankedDate()).withZone(DateTimeZone.UTC).toDate())
            .paymentChannel(bulkScanPaymentRequest.getPaymentChannel())
            .documentControlNumber(bulkScanPaymentRequest.getDocumentControlNumber())
            .payerName(bulkScanPaymentRequest.getPayerName())
            .paymentStatus(bulkScanPaymentRequest.getPaymentStatus())
            .bankedDate(DateTime.parse(bulkScanPaymentRequest.getBankedDate()).withZone(DateTimeZone.UTC).toDate())
            .build();

        PaymentFeeLink paymentFeeLink = paymentGroupService.addNewBulkScanPayment(payment, paymentGroupReference);

        Payment newPayment = getPayment(paymentFeeLink, payment.getReference());

        return new ResponseEntity<>(paymentDtoMapper.toBulkScanPaymentDto(newPayment, paymentGroupReference), HttpStatus.CREATED);
    }

    private Payment getPayment(PaymentFeeLink paymentFeeLink, String paymentReference){
        return paymentFeeLink.getPayments().stream().filter(p -> p.getReference().equals(paymentReference)).findAny()
            .orElseThrow(() -> new PaymentNotFoundException("Payment with reference " + paymentReference + " does not exists."));
    }

    private void updateFeeAmountDue(Payment payment) {
        Optional<List<FeePayApportion>> apportions = feePayApportionRepository.findByPaymentId(payment.getId());
        if(apportions.isPresent()) {
            apportions.get().stream()
                .forEach(feePayApportion -> {
                    PaymentFee fee = paymentFeeRepository.findById(feePayApportion.getFeeId()).get();
                    if(feePayApportion.getCallSurplusAmount() != null) {
                        feePayApportion.setCallSurplusAmount(feePayApportion.getCallSurplusAmount());
                    }else {
                        feePayApportion.setCallSurplusAmount(BigDecimal.valueOf(0));
                    }
                    fee.setAmountDue(fee.getAmountDue().subtract(feePayApportion.getApportionAmount()
                        .add(feePayApportion.getCallSurplusAmount())));
                    if(fee.getAmountDue().intValue() <= 0){
                        fee.setIsFullyApportioned("Y");
                    }
                    paymentFeeRepository.save(fee);
                    LOG.info("Updated FeeId " + fee.getId() + " as PaymentId " + payment.getId() + " Status Changed to " + payment.getPaymentStatus().getName());
                });
        }
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
    @ExceptionHandler(MethodNotSupportedException.class)
    public String return400(MethodNotSupportedException ex) {
        return ex.getMessage();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PaymentException.class)
    public String return400(PaymentException ex) {
        return ex.getMessage();
    }
}
