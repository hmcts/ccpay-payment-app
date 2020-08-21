package uk.gov.hmcts.payment.api.controllers;

import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentGroupDtoMapper;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.service.PaymentService;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import java.util.List;
import java.util.Optional;

@RestController
@Api(tags = {"PaymentApportion"})
@SwaggerDefinition(tags = {@Tag(name = "FeePayApportionController", description = "FeePayApportion REST API")})
public class FeePayApportionController {

    private final PaymentService<PaymentFeeLink, String> paymentService;

    private final PaymentFeeRepository paymentFeeRepository;

    private final PaymentGroupDtoMapper paymentGroupDtoMapper;

    @Autowired
    private LaunchDarklyFeatureToggler featureToggler;

    private static final Logger LOG = LoggerFactory.getLogger(FeePayApportionController.class);

    @Autowired
    public FeePayApportionController(PaymentService<PaymentFeeLink, String> paymentService,PaymentFeeRepository paymentFeeRepository,PaymentGroupDtoMapper paymentGroupDtoMapper,LaunchDarklyFeatureToggler featureToggler) {
        this.paymentService = paymentService;
        this.paymentFeeRepository = paymentFeeRepository;
        this.paymentGroupDtoMapper = paymentGroupDtoMapper;
        this.featureToggler = featureToggler;
    }

    @ApiOperation(value = "Get apportion details by payment reference", notes = "Get apportion details for supplied payment reference")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Apportionment Details retrieved"),
        @ApiResponse(code = 403, message = "Payment info forbidden"),
        @ApiResponse(code = 404, message = "Payment not found")
    })
    @GetMapping(value = "/payment-groups/fee-pay-apportion/{paymentreference}")
    public ResponseEntity<PaymentGroupDto> retrieveApportionDetails(@PathVariable("paymentreference") String paymentReference) {
        LOG.info("Invoking new API in FeePayApportionController");
        PaymentFeeLink paymentFeeLink = paymentService.retrieve(paymentReference);
        boolean apportionFeature = featureToggler.getBooleanValue("apportion-feature",false);
        LOG.info("apportionFeature value in FeePayApportionController: {}", apportionFeature);
        Optional<Payment> payment = paymentFeeLink.getPayments().stream()
            .filter(p -> p.getReference().equals(paymentReference)).findAny();
        List<PaymentFee> feeList = paymentFeeLink.getFees();
        if (payment.isPresent() && apportionFeature)
        {
            LOG.info("Apportion feature is true and payment is available in FeePayApportionController");
            List<FeePayApportion> feePayApportionList = paymentService.findByPaymentId(payment.get().getId());
            if(feePayApportionList != null && !feePayApportionList.isEmpty()) {
                LOG.info("Apportion details available in FeePayApportionController");
                feePayApportionList.stream()
                    .forEach(feePayApportion -> {
                        feeList.stream()
                            .forEach(paymentFee -> {
                                if (feePayApportion.getFeeId().equals(paymentFee.getId())) {
                                    LOG.info("Fee id matched in FeePayApportionController");
                                    PaymentFee fee = paymentFeeRepository.findById(feePayApportion.getFeeId()).get();
                                    fee.setApportionAmount(feePayApportion.getApportionAmount());
                                }
                            });
                    });
                paymentFeeLink.setFees(feeList);
            }

    }
        return new ResponseEntity<>(paymentGroupDtoMapper.toPaymentGroupDto(paymentFeeLink), HttpStatus.OK);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(PaymentNotFoundException.class)
    public String notFound(PaymentNotFoundException ex) {
        return ex.getMessage();
    }
}
