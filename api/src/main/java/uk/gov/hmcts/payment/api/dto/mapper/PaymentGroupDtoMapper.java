package uk.gov.hmcts.payment.api.dto.mapper;

import com.google.common.collect.Streams;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.fees2.register.api.contract.FeeVersionDto;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.contract.DisputeDto;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentAllocationDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.RemissionDto;
import uk.gov.hmcts.payment.api.model.FeePayApportion;
import uk.gov.hmcts.payment.api.model.FeePayApportionRepository;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentAllocation;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeRepository;
import uk.gov.hmcts.payment.api.model.Remission;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.reports.FeesService;
import uk.gov.hmcts.payment.api.service.RefundRemissionEnableService;
import uk.gov.hmcts.payment.api.util.PayStatusToPayHubStatus;
import uk.gov.hmcts.payment.api.util.ServiceRequestUtil;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PaymentGroupDtoMapper {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentGroupDtoMapper.class);

    @Autowired
    private FeesService feesService;

    @Autowired
    private LaunchDarklyFeatureToggler featureToggler;

    @Autowired
    private PaymentFeeRepository paymentFeeRepository;

    @Autowired
    private RefundRemissionEnableService refundRemissionEnableService;

    @Autowired
    private FeePayApportionRepository feePayApportionRepository;


    @Autowired
   private  PaymentFailureRepository paymentFailureRepository;

    private static final int FIRSTPING = 1;
    private static final int SECONDPING = 2;

    Optional<List<PaymentFailures>> paymentFailuresList;

    public PaymentGroupDto toPaymentGroupDto(PaymentFeeLink paymentFeeLink) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean containsPaymentRole = false;
        ServiceRequestUtil serviceRequestUtil = new ServiceRequestUtil();

        Iterator<? extends GrantedAuthority> userRole =  authentication.getAuthorities().iterator();

        while (userRole.hasNext()){
            if(userRole.next().toString().equals("payments")){
                containsPaymentRole = true;
                break;
            }
        }

        List<String> paymentReference = paymentFeeLink.getPayments().stream().map(Payment::getReference).collect(Collectors.toList());

        paymentFailuresList = paymentFailureRepository.findByPaymentReferenceIn(paymentReference);
        PaymentGroupDto paymentGroupDto;

        paymentGroupDto = PaymentGroupDto.paymentGroupDtoWith()
            .paymentGroupReference(paymentFeeLink.getPaymentReference())
            .dateCreated(paymentFeeLink.getDateCreated())
            .dateUpdated(paymentFeeLink.getDateUpdated())
            .fees(toFeeDtos(paymentFeeLink.getFees()))
            .payments((!(paymentFeeLink.getPayments() == null) && !paymentFeeLink.getPayments().isEmpty()) ? toPaymentDtos(paymentFeeLink.getPayments()) : null)
            .remissions(!(paymentFeeLink.getRemissions() == null) ? toRemissionDtos(paymentFeeLink.getRemissions()) : null)
            .build();

        if (null != paymentGroupDto.getPayments()) {
            for (PaymentDto paymentDto : paymentGroupDto.getPayments()) {
                for (DisputeDto disputeDTO : paymentDto.getDisputes()) {

                    if (disputeDTO.isDispute() && disputeDTO.getPingNumber() == FIRSTPING) {

                        paymentGroupDto.setAnyPaymentDisputed(true);
                        break;
                    }
                }

            }
        }

        String serviceRequestStatus = serviceRequestUtil.getServiceRequestStatus(paymentGroupDto);

        if(!containsPaymentRole){
            paymentGroupDto = PaymentGroupDto.paymentGroupDtoWith()
                .paymentGroupReference(paymentFeeLink.getPaymentReference())
                .dateCreated(paymentFeeLink.getDateCreated())
                .dateUpdated(paymentFeeLink.getDateUpdated())
                .fees(toFeeDtos(paymentFeeLink.getFees()))
                .build();
        }
        paymentGroupDto.setServiceRequestStatus(serviceRequestStatus);

        return paymentGroupDto;
    }

    private List<PaymentDto> toPaymentDtos(List<Payment> payments) {
        return payments.stream().map(p -> toPaymentDto(p)).collect(Collectors.toList());
    }
    //added missing pba account details
    private PaymentDto toPaymentDto(Payment payment) {
        return PaymentDto.payment2DtoWith()
            .reference(payment.getReference())
            .amount(payment.getAmount())
            .currency(CurrencyCode.valueOf(payment.getCurrency()))
            .caseReference(payment.getCaseReference())
            .ccdCaseNumber(payment.getCcdCaseNumber())
            .accountNumber(payment.getPbaNumber())
            .organisationName(payment.getOrganisationName())
            .customerReference(payment.getCustomerReference())
            .status(PayStatusToPayHubStatus.valueOf(payment.getPaymentStatus().getName()).getMappedStatus())
            .serviceName(payment.getServiceType())
            .siteId(payment.getSiteId())
            .description(payment.getDescription())
            .channel(payment.getPaymentChannel() != null ? payment.getPaymentChannel().getName() : null)
            .method(payment.getPaymentMethod() != null ? payment.getPaymentMethod().getName() : null)
            .externalReference(payment.getExternalReference())
            .externalProvider(payment.getPaymentProvider() != null ? payment.getPaymentProvider().getName() : null)
            .dateCreated(payment.getDateCreated() != null ? payment.getDateCreated() : null)
            .dateUpdated(payment.getDateUpdated() != null ? payment.getDateUpdated() : null)
            .documentControlNumber(payment.getDocumentControlNumber())
            .bankedDate(payment.getBankedDate())
            .payerName(payment.getPayerName())
            .refundEnable(payment.getDateUpdated() != null ? toRefundEligible(payment):false)
            .paymentAllocation(payment.getPaymentAllocation() !=null ? toPaymentAllocationDtos(payment.getPaymentAllocation()) : null)
            .overPayment(setOverpaymentObj(payment.getId()))
            .disputes(evaluatePaymentDispute(payment))
            .build();
    }

    private BigDecimal setOverpaymentObj(Integer id) {
        AtomicReference<BigDecimal> overpayment = new AtomicReference<>(BigDecimal.ZERO);
        Optional<List<FeePayApportion>> feepayapplist = feePayApportionRepository.findByPaymentId(id);

        if(feepayapplist.isPresent()){
            List<FeePayApportion> feeList = feepayapplist.get()
                .stream()
                .filter(c -> (c.getCallSurplusAmount() !=null && c.getCallSurplusAmount().intValue()> 0))
                .collect(Collectors.toList());

            if (!feeList.isEmpty()) {
                overpayment.set(feeList.get(0).getCallSurplusAmount());
            }
        }
        return overpayment.get();
    }

    private List<RemissionDto> toRemissionDtos(List<Remission> remissions) {
        return remissions.stream().map(r -> toRemissionDto(r)).collect(Collectors.toList());
    }

    private List<PaymentAllocationDto> toPaymentAllocationDtos(List<PaymentAllocation> paymentAllocation) {
        return paymentAllocation.stream().map(pa -> toPaymentAllocationDto(pa)).collect(Collectors.toList());
    }

    public PaymentAllocationDto toPaymentAllocationDto(PaymentAllocation paymentAllocation){
        return PaymentAllocationDto.paymentAllocationDtoWith()
            .allocationStatus(paymentAllocation.getPaymentAllocationStatus().getName())
            .build();
    }
    private BigDecimal getTotalHwfRemission(List<Remission> remissions) {
        return remissions != null ? remissions.stream().map(Remission::getHwfAmount).reduce(BigDecimal.ZERO, BigDecimal::add) : new BigDecimal("0.00");
    }

    private RemissionDto toRemissionDto(Remission remission) {
        return RemissionDto.remissionDtoWith()
            .feeId(remission.getFee().getId())
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
        LOG.info("Inside toFeeDto and amount due is: {}", fee.getAmountDue());
        return FeeDto.feeDtoWith()
            .calculatedAmount(fee.getCalculatedAmount())
            .code(fee.getCode())
            .netAmount(fee.getNetAmount())
            .version(fee.getVersion())
            .volume(fee.getVolume())
            .feeAmount(fee.getFeeAmount())
            .ccdCaseNumber(fee.getCcdCaseNumber())
            .reference(fee.getReference())
            .id(fee.getId())
            .memoLine(optionalFeeVersionDto.isPresent() ? optionalFeeVersionDto.get().getMemoLine() : null)
            .naturalAccountCode(optionalFeeVersionDto.isPresent() ? optionalFeeVersionDto.get().getNaturalAccountCode() : null)
            .description( optionalFeeVersionDto.isPresent() ? optionalFeeVersionDto.get().getDescription() : null)
            .allocatedAmount(fee.getAllocatedAmount())
            .apportionAmount(fee.getApportionAmount())
            .dateCreated(fee.getDateCreated())
            .dateUpdated(fee.getDateUpdated())
            .dateApportioned(fee.getDateApportioned())
            .amountDue(fee.getAmountDue())
            .overPayment(setOverpayment(fee))
            .remissionEnable(toRemissionEnable(fee))
            .netAmount(fee.getNetAmount())
            .build();
    }

    public PaymentFee toPaymentFee(FeeDto feeDto){
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        boolean apportionFeature = featureToggler.getBooleanValue("apportion-feature",false);
        return PaymentFee.feeWith()
            .code(feeDto.getCode())
            .version(feeDto.getVersion())
            .calculatedAmount(feeDto.getCalculatedAmount())
            .ccdCaseNumber(feeDto.getCcdCaseNumber())
            .volume(feeDto.getVolume())
            .feeAmount(feeDto.getFeeAmount())
            .netAmount(feeDto.getCalculatedAmount())
            .reference(feeDto.getReference())
            .dateCreated(apportionFeature ? timestamp: null)
            .build();
    }

    private Boolean toRefundEligible(Payment payment) {

        return refundRemissionEnableService.returnRefundEligible(payment);
    }

    private Boolean toRemissionEnable(PaymentFee fee){

        return refundRemissionEnableService.returnRemissionEligible(fee);
    }

    private BigDecimal setOverpayment(PaymentFee fee){
        BigDecimal overpayment =  BigDecimal.ZERO;
        Optional<List<FeePayApportion>> feepayapplist = feePayApportionRepository.findByFeeId(fee.getId());

        if(feepayapplist.isPresent() && !feepayapplist.get().isEmpty()) {
            List<FeePayApportion> feeList = feepayapplist.get()
                .stream()
                .filter(c -> (c.getCallSurplusAmount() !=null && c.getCallSurplusAmount().intValue()> 0))
                .collect(Collectors.toList());
            if (!feeList.isEmpty()) {
                Optional<FeePayApportion> feepayapp = feePayApportionRepository.findByFeeIdAndPaymentId(feeList.get(0).getFeeId(), feeList.get(0).getPaymentId());
                if (feepayapp.isPresent()) {
                    overpayment = feepayapp.get().getCallSurplusAmount();
                }
            }
        }
        return overpayment;
    }

    private List<DisputeDto> evaluatePaymentDispute(Payment payment) {

        List<DisputeDto> disputeDTOs = new ArrayList<>();

        if (paymentFailuresList.isPresent()) {
            for (PaymentFailures paymentFailure: paymentFailuresList.get()) {
                if (paymentFailure.getPaymentReference().equals(payment.getReference())) {
                    DisputeDto disputeDTO = new DisputeDto();
                    if (paymentFailure.getRepresentmentSuccess() == null) {
                        disputeDTO.setDispute(true);
                        disputeDTO.setPingNumber(FIRSTPING);
                    } else if (paymentFailure.getRepresentmentSuccess().equalsIgnoreCase("Yes")) {
                        disputeDTO.setDispute(false);
                        disputeDTO.setPingNumber(SECONDPING);
                    } else {
                        disputeDTO.setDispute(true);
                        disputeDTO.setPingNumber(SECONDPING);
                    }
                    disputeDTO.setAmount(paymentFailure.getAmount());
                    disputeDTO.setDcn(paymentFailure.getDcn());
                    disputeDTO.setCcdCaseNumber(paymentFailure.getCcdCaseNumber());
                    disputeDTO.setPaymentReference(paymentFailure.getPaymentReference());
                    disputeDTO.setFailureEventDateTime(paymentFailure.getFailureEventDateTime());
                    disputeDTO.setFailureReference(paymentFailure.getFailureReference());
                    disputeDTO.setFailureType(paymentFailure.getFailureType());
                    disputeDTO.setHasAmountDebited(paymentFailure.getHasAmountDebited());
                    disputeDTO.setReason(paymentFailure.getReason());
                    disputeDTO.setRepresentmentOutcomeDate(paymentFailure.getRepresentmentOutcomeDate());
                    disputeDTO.setRepresentmentSuccess(paymentFailure.getRepresentmentSuccess());
                    disputeDTOs.add(disputeDTO);
                }
            }
        } else {
            DisputeDto disputeDTO = new DisputeDto();
            disputeDTO.setDispute(false);
            disputeDTO.setPingNumber(0);
            disputeDTOs.add(disputeDTO);
        }

        return disputeDTOs;
    }

    public PaymentGroupDto calculateOverallBalance(PaymentGroupDto paymentGroupDto){

        if (paymentGroupDto.getRemissions() == null || paymentGroupDto.getPayments() == null || paymentGroupDto.getFees() == null) {
            return paymentGroupDto;
        }
        final var remissions= paymentGroupDto.getRemissions().iterator();
        final var payments= paymentGroupDto.getPayments().iterator();
        final var fees= paymentGroupDto.getFees().iterator();

        while (remissions.hasNext() && payments.hasNext() && fees.hasNext()) {
            final var remission = remissions.next();
            remission.setOverallBalance(
                payments.next().getAmount().subtract(
                    fees.next().getCalculatedAmount().subtract(remission.getHwfAmount())
                ));
        }
        return paymentGroupDto;
    }
}
