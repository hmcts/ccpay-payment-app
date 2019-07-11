package uk.gov.hmcts.payment.api.dto.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.fees2.register.api.contract.FeeVersionDto;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.RemissionDto;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.Remission;
import uk.gov.hmcts.payment.api.reports.FeesService;
import uk.gov.hmcts.payment.api.util.PayStatusToPayHubStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PaymentGroupDtoMapper {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentGroupDtoMapper.class);

    @Autowired
    private FeesService feesService;

    private BigDecimal totalHwfAmount;

    public PaymentGroupDto toPaymentGroupDto(PaymentFeeLink paymentFeeLink) {
        totalHwfAmount = getTotalHwfRemission(paymentFeeLink.getRemissions());
        return PaymentGroupDto.paymentGroupDtoWith()
            .paymentGroupReference(paymentFeeLink.getPaymentReference())
            .fees(toFeeDtos(paymentFeeLink.getFees()))
            .payments((!(paymentFeeLink.getPayments() == null) && !paymentFeeLink.getPayments().isEmpty()) ? toPaymentDtos(paymentFeeLink.getPayments()) : null)
            .remissions(!(paymentFeeLink.getRemissions() == null) ? toRemissionDtos(paymentFeeLink.getRemissions()) : null)
            .build();
    }

    private List<PaymentDto> toPaymentDtos(List<Payment> payments) {
        return payments.stream().map(p -> toPaymentDto(p)).collect(Collectors.toList());
    }

    private PaymentDto toPaymentDto(Payment payment) {
        return PaymentDto.payment2DtoWith()
            .reference(payment.getReference())
            .amount(payment.getAmount())
            .currency(CurrencyCode.valueOf(payment.getCurrency()))
            .caseReference(payment.getCaseReference())
            .ccdCaseNumber(payment.getCcdCaseNumber())
            .status(PayStatusToPayHubStatus.valueOf(payment.getPaymentStatus().getName()).mapedStatus)
            .serviceName(payment.getServiceType())
            .siteId(payment.getSiteId())
            .description(payment.getDescription())
            .channel(payment.getPaymentChannel() != null ? payment.getPaymentChannel().getName() : null)
            .method(payment.getPaymentMethod() != null ? payment.getPaymentMethod().getName() : null)
            .externalReference(payment.getExternalReference())
            .externalProvider(payment.getPaymentProvider() != null ? payment.getPaymentProvider().getName() : null)
            .build();
    }

    private List<RemissionDto> toRemissionDtos(List<Remission> remissions) {
        return remissions.stream().map(r -> toRemissionDto(r)).collect(Collectors.toList());
    }

    private BigDecimal getTotalHwfRemission(List<Remission> remissions) {
        return remissions != null ? remissions.stream().map(Remission::getHwfAmount).reduce(BigDecimal.ZERO, BigDecimal::add) : new BigDecimal("0.00");
    }

    private RemissionDto toRemissionDto(Remission remission) {
        return RemissionDto.remissionDtoWith()
            .remissionReference(remission.getRemissionReference())
            .beneficiaryName(remission.getBeneficiaryName())
            .ccdCaseNumber(remission.getCcdCaseNumber())
            .caseReference(remission.getCaseReference())
            .hwfReference(remission.getHwfReference())
            .hwfAmount(remission.getHwfAmount())
            .feeCode(remission.getFee().getCode())
            .dateCreated(remission.getDateCreated())
            .build();
    }

    private List<FeeDto> toFeeDtos(List<PaymentFee> paymentFees) {
        return paymentFees.stream().map(f -> toFeeDto(f)).collect(Collectors.toList());
    }

    private FeeDto toFeeDto(PaymentFee fee) {

        Optional<FeeVersionDto> optionalFeeVersionDto = feesService.getFeeVersion(fee.getCode(), fee.getVersion());

        return FeeDto.feeDtoWith()
            .calculatedAmount(fee.getCalculatedAmount())
            .code(fee.getCode())
            .netAmount(fee.getCalculatedAmount().subtract(totalHwfAmount != null ? totalHwfAmount : new BigDecimal(0)))
            .version(fee.getVersion())
            .volume(fee.getVolume())
            .ccdCaseNumber(fee.getCcdCaseNumber())
            .reference(fee.getReference())
            .id(fee.getId())
            .memoLine(optionalFeeVersionDto.isPresent() ? optionalFeeVersionDto.get().getMemoLine() : null)
            .naturalAccountCode(optionalFeeVersionDto.isPresent() ? optionalFeeVersionDto.get().getNaturalAccountCode() : null)
            .description( optionalFeeVersionDto.isPresent() ? optionalFeeVersionDto.get().getDescription() : null)
            .build();
    }

    public PaymentFee toPaymentFee(FeeDto feeDto){
        return PaymentFee.feeWith()
            .code(feeDto.getCode())
            .version(feeDto.getVersion())
            .calculatedAmount(feeDto.getCalculatedAmount())
            .ccdCaseNumber(feeDto.getCcdCaseNumber())
            .volume(feeDto.getVolume())
            .netAmount(feeDto.getNetAmount())
            .reference(feeDto.getReference())
            .build();
    }
}
