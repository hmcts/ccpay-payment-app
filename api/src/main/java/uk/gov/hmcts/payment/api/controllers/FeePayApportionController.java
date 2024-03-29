package uk.gov.hmcts.payment.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentGroupDtoMapper;
import uk.gov.hmcts.payment.api.model.FeePayApportion;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeRepository;
import uk.gov.hmcts.payment.api.service.PaymentRefundsService;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.service.RefundRemissionEnableService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@Tag(name = "FeePayApportionController", description = "FeePayApportion REST API")
public class FeePayApportionController {

    private final PaymentService<PaymentFeeLink, String> paymentService;

    private final PaymentFeeRepository paymentFeeRepository;

    private final PaymentGroupDtoMapper paymentGroupDtoMapper;

    @Autowired
    private LaunchDarklyFeatureToggler featureToggler;

    @Autowired
    private PaymentRefundsService paymentRefundsService;

    @Autowired
    private RefundRemissionEnableService refundRemissionEnableService;

    private static final Logger LOG = LoggerFactory.getLogger(FeePayApportionController.class);

    @Autowired
    public FeePayApportionController(PaymentService<PaymentFeeLink, String> paymentService,PaymentFeeRepository paymentFeeRepository,PaymentGroupDtoMapper paymentGroupDtoMapper,LaunchDarklyFeatureToggler featureToggler) {
        this.paymentService = paymentService;
        this.paymentFeeRepository = paymentFeeRepository;
        this.paymentGroupDtoMapper = paymentGroupDtoMapper;
        this.featureToggler = featureToggler;
    }

    @Operation(summary = "Get apportion details by payment reference", description = "Get apportion details for supplied payment reference")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Apportionment Details retrieved"),
        @ApiResponse(responseCode = "403", description = "Payment info forbidden"),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping(value = "/payment-groups/fee-pay-apportion/{paymentreference}")
    public ResponseEntity<PaymentGroupDto> retrieveApportionDetails(@PathVariable("paymentreference") String paymentReference,@RequestHeader(required = false) MultiValueMap<String, String> headers) {
        LOG.info("Invoking GET API retrieveApportionDetails in FeePayApportionController {}", paymentReference);
        PaymentFeeLink paymentFeeLink = paymentService.retrievePayment(paymentReference);
        boolean apportionFeature = featureToggler.getBooleanValue("apportion-feature",false);
        LOG.info("apportionFeature value in FeePayApportionController: {}", apportionFeature);
        Optional<Payment> payment = paymentFeeLink.getPayments().stream()
            .filter(p -> p.getReference().equals(paymentReference)).findAny();

        if (payment.isPresent() && apportionFeature)
        {
            LOG.info("Apportion feature is true and payment is available in FeePayApportionController");
            List<FeePayApportion> feePayApportionList = paymentService.findByPaymentId(payment.get().getId());
            if(feePayApportionList != null && !feePayApportionList.isEmpty()) {
                LOG.info("Apportion details available in FeePayApportionController size {}", feePayApportionList.size());
                List<Integer> feeIdList = feePayApportionList.stream().map(FeePayApportion::getFeeId).collect(Collectors.toList());
                LOG.info("feeIdList size {}", feeIdList.size());
                List<PaymentFee> feeList = new ArrayList<>();
                List<PaymentFee> paymentFeeList = paymentFeeRepository.findByIdIn(feeIdList);
                LOG.info("paymentFeeList Size {}", paymentFeeList.size());
                for (FeePayApportion feePayApportion : feePayApportionList)
                {
                    LOG.info("Inside FeePayApportion section in FeePayApportionController");
                    LOG.info("FeePayApportion FeeId {}", feePayApportion.getFeeId());
                    Optional<PaymentFee> apportionedFee = Optional.ofNullable(paymentFeeList.stream().filter
                        (e->e.getId().equals(feePayApportion.getFeeId())).collect(Collectors.toList()).get(0));
                    if(apportionedFee.isPresent()) {
                        LOG.info("Apportioned fee is present");
                        PaymentFee fee = apportionedFee.get();
                        LOG.info("apportion amount value in FeePayApportionController: {}", feePayApportion.getApportionAmount());
                        fee.setApportionAmount(feePayApportion.getApportionAmount());
                        feeList.add(fee);
                    }
                }
                LOG.info("feeList size {}", feeList.size());
                paymentFeeLink.setFees(feeList);
            }
        }
        LOG.info("Before calling toPaymentGroupDto payment Ref {}", paymentFeeLink.getPaymentReference());
        refundRemissionEnableService.setUserRoles(headers);
        PaymentGroupDto paymentGroupDto  = paymentGroupDtoMapper.toPaymentGroupDto(paymentFeeLink);
        LOG.info("Before checking Refund");
        paymentGroupDto = paymentRefundsService.checkRefundAgainstRemissionFeeApportionV2(headers, paymentGroupDto, paymentReference);
        paymentGroupDtoMapper.calculateOverallBalance(paymentGroupDto);
        return new ResponseEntity<>(paymentGroupDto, HttpStatus.OK);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(PaymentNotFoundException.class)
    public String notFound(PaymentNotFoundException ex) {
        return ex.getMessage();
    }
}
