package uk.gov.hmcts.payment.api.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.payment.api.audit.AuditRepository;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.dto.PaymentSearchCriteria;
import uk.gov.hmcts.payment.api.dto.Reference;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.*;


@Service
public class PaymentServiceImpl implements PaymentService<PaymentFeeLink, String> {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private static final PaymentStatus SUCCESS = new PaymentStatus("success", "success");
    private static final PaymentStatus FAILED = new PaymentStatus("failed", "failed");
    private static final PaymentStatus CANCELLED = new PaymentStatus("cancelled", "cancelled");
    private static final PaymentStatus ERROR = new PaymentStatus("error", "error");
    private static final String PCI_PAL = "pci pal";

    private final Payment2Repository paymentRepository;
    private final DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService;
    private final CallbackService callbackService;
    private final PaymentStatusRepository paymentStatusRepository;
    private final TelephonyRepository telephonyRepository;
    private final AuditRepository paymentAuditRepository;
    private final FeePayApportionRepository feePayApportionRepository;
    private final PaymentFeeRepository paymentFeeRepository;
    private final LaunchDarklyFeatureToggler featureToggler;

    @Value("${callback.payments.cutoff.time.in.minutes:2}")
    private int paymentsCutOffTime;

    @Autowired
    public PaymentServiceImpl(@Qualifier("loggingPaymentService") DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService,
                              Payment2Repository paymentRepository, CallbackService callbackService, PaymentStatusRepository paymentStatusRepository,
                              TelephonyRepository telephonyRepository, AuditRepository paymentAuditRepository,
                              FeePayApportionRepository feePayApportionRepository,
                              PaymentFeeRepository paymentFeeRepository,LaunchDarklyFeatureToggler featureToggler) {
        this.paymentRepository = paymentRepository;
        this.delegatingPaymentService = delegatingPaymentService;
        this.callbackService = callbackService;
        this.paymentStatusRepository = paymentStatusRepository;
        this.telephonyRepository = telephonyRepository;
        this.paymentAuditRepository = paymentAuditRepository;
        this.feePayApportionRepository = feePayApportionRepository;
        this.paymentFeeRepository = paymentFeeRepository;
        this.featureToggler = featureToggler;
    }

    @Override
    public PaymentFeeLink retrieve(String reference) {
        Payment payment = findSavedPayment(reference);

        return payment.getPaymentLink();
    }

    @Override
    @Transactional
    public void updateTelephonyPaymentStatus(String paymentReference, String status, String payload) {
        Payment payment = paymentRepository.findByReferenceAndPaymentProvider(paymentReference,
            PaymentProvider.paymentProviderWith().name(PCI_PAL).build()).orElseThrow(PaymentNotFoundException::new);
        if(payment.getPaymentStatus() != null && !payment.getPaymentStatus().getName().equalsIgnoreCase(PaymentStatus.SUCCESS.getName())){
            payment.setPaymentStatus(paymentStatusRepository.findByNameOrThrow(status));

            payment.setStatusHistories(Collections.singletonList(StatusHistory.statusHistoryWith()
                .status(status)
                .build()));
            if (payment.getServiceCallbackUrl() != null) {
                callbackService.callback(payment.getPaymentLink(), payment);
            }
            telephonyRepository.save(TelephonyCallback.telephonyCallbackWith().paymentReference(paymentReference).payload(payload).build());
            //1. Update Fee Amount Due as Payment Status received from PCI PAL as SUCCESS
            //2. Rollback Fees already Apportioned for Payments in FAILED status based on launch darkly feature flag
            boolean apportionFeature = featureToggler.getBooleanValue("apportion-feature",false);
            LOG.info("ApportionFeature Flag Value in UserAwareDelegatingPaymentService : {}", apportionFeature);
            if(apportionFeature) {
                if (Lists.newArrayList("failed", "cancelled", "error", "decline").contains(status)) {
                    LOG.info("Rollback Fees already Apportioned for Payments in failed/cancelled/error status - Started");
                    rollbackApportionedFees(payment);
                    LOG.info("Rollback Fees already Apportioned for Payments in failed/cancelled/error status - End");
                }
                if(status.equalsIgnoreCase("success")) {
                    LOG.info("Update Fee Amount Due as Payment Status received from GovPAY as SUCCESS!!!");
                    updateFeeAmountDue(payment);
                }
            }
        }else {
            Map<String, String> properties = new ImmutableMap.Builder<String, String>()
                .put("PaymentReference", paymentReference)
                .put("RequestedStatus", status)
                .put("Payload", payload)
                .put("ExistingStatus", payment.getPaymentStatus().getName())
                .build();
            paymentAuditRepository.trackEvent("DUPLICATE_STATUS_UPDATE", properties);
        }
    }

    @Override
    public List<Payment> getPayments(Date atStartOfDay, Date atEndOfDay) {
        return paymentRepository.findAllByDateCreatedBetween(atStartOfDay, atEndOfDay).orElse(Collections.EMPTY_LIST);
    }

    @Override
    public List<Reference> listInitiatedStatusPaymentsReferences() {
        Date targetTime = DateUtils.addMinutes(new Date(), -1 * paymentsCutOffTime);
        return paymentRepository.findReferencesByPaymentProviderAndPaymentStatusNotInAndDateCreatedLessThan(
            PaymentProvider.GOV_PAY,
            Lists.newArrayList(SUCCESS, FAILED, ERROR, CANCELLED), targetTime
        );
    }

    @Override
    public List<PaymentFeeLink> search(PaymentSearchCriteria searchCriteria) {
        return delegatingPaymentService.search(searchCriteria);
    }

    @Override
    public List<FeePayApportion> findByPaymentId(Integer paymentId)
    {
        return feePayApportionRepository.findByPaymentId(paymentId).orElse(Collections.EMPTY_LIST);
    }

    private Payment findSavedPayment(@NotNull String paymentReference) {
        return paymentRepository.findByReference(paymentReference).orElseThrow(PaymentNotFoundException::new);
    }

    private void updateFeeAmountDue(Payment payment) {
        Optional<List<FeePayApportion>> apportions = feePayApportionRepository.findByPaymentId(payment.getId());
        if(apportions.isPresent()) {
            apportions.get().stream()
                .forEach(feePayApportion -> {
                    PaymentFee fee = paymentFeeRepository.findById(feePayApportion.getFeeId()).get();
                    fee.setAmountDue(fee.getAmountDue().subtract(feePayApportion.getApportionAmount()));
                    paymentFeeRepository.save(fee);
                    LOG.info("Updated FeeId " + fee.getId() + " as PaymentId " + payment.getId() + " Status Changed to " + payment.getPaymentStatus().getName());
                });
        }
    }

    private void rollbackApportionedFees(Payment payment) {
        Optional<List<FeePayApportion>> apportions = feePayApportionRepository.findByPaymentId(payment.getId());
        if(apportions.isPresent()) {
            apportions.get().stream()
                .forEach(feePayApportion -> {
                    PaymentFee fee = paymentFeeRepository.findById(feePayApportion.getFeeId()).get();
                    fee.setIsFullyApportioned("N");
                    fee.setApportionAmount(fee.getApportionAmount().subtract(feePayApportion.getApportionAmount()));
                    // If Fee was surplus due to Payment received, the whole allocated amount to be reverted else only last Apportioned Amount to be reverted
                    if(feePayApportion.getCallSurplusAmount() != null) {
                        feePayApportion.setCallSurplusAmount(feePayApportion.getCallSurplusAmount());
                    }else {
                        feePayApportion.setCallSurplusAmount(BigDecimal.valueOf(0));
                    }
                    if(feePayApportion.getCallSurplusAmount().compareTo(BigDecimal.valueOf(0)) > 0) {
                        fee.setAllocatedAmount(fee.getAllocatedAmount()
                            .subtract(feePayApportion.getApportionAmount()
                                .add(feePayApportion.getCallSurplusAmount())));
                    }else {
                        fee.setAllocatedAmount(fee.getAllocatedAmount().subtract(feePayApportion.getApportionAmount()));
                    }
                    paymentFeeRepository.save(fee);
                    LOG.info("Rollback FeeId " + fee.getId() + " as PaymentId " + payment.getId() + " Status Changed to " + payment.getPaymentStatus().getName());
                });
        }
    }

}
