package uk.gov.hmcts.payment.api.service;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.payment.api.dto.FeePayApportionCCDCase;
import uk.gov.hmcts.payment.api.model.*;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FeePayApportionServiceImpl implements FeePayApportionService {

    private static final Logger LOG = LoggerFactory.getLogger(FeePayApportionServiceImpl.class);

    private final PaymentFeeLinkRepository paymentFeeLinkRepository;

    private final PaymentStatusRepository paymentStatusRepository;

    private final FeePayStagedRepository feePayStagedRepository;

    private final FeePayApportionRepository feePayApportionRepository;

    private final PaymentFeeRepository paymentFeeRepository;

    private final String APPORTION_GO_LIVE_DATE = "01.06.2020";

    private Boolean isSurplus = false;
    private Boolean isShortfall = false;

    public FeePayApportionServiceImpl(PaymentFeeLinkRepository paymentFeeLinkRepository,
                                      PaymentStatusRepository paymentStatusRepository,
                                      FeePayStagedRepository feePayStagedRepository,
                                      FeePayApportionRepository feePayApportionRepository,
                                      PaymentFeeRepository paymentFeeRepository) {
        this.paymentFeeLinkRepository = paymentFeeLinkRepository;
        this.paymentStatusRepository = paymentStatusRepository;
        this.feePayStagedRepository = feePayStagedRepository;
        this.feePayApportionRepository = feePayApportionRepository;
        this.paymentFeeRepository = paymentFeeRepository;
    }

    @Override
    @Transactional
    public FeePayApportionCCDCase processFeePayApportion(FeePayApportionCCDCase feePayApportionCCDCase) {

        BigDecimal callShortfallAmount = new BigDecimal(0);
        BigDecimal callSurplusAmount = new BigDecimal(0);
        BigDecimal remainingPaymentAmount = new BigDecimal(0);

        if( !getPaymentsToBeApportioned(feePayApportionCCDCase.getPayments()).isEmpty() &&
                !getFeesToBeApportioned(feePayApportionCCDCase.getFees()).isEmpty()) {
            List<FeePayApportion> feePayApportions = new ArrayList<>();

            for(Payment payment : getPaymentsToBeApportioned(feePayApportionCCDCase.getPayments())) {
                remainingPaymentAmount = payment.getAmount();

                for(PaymentFee fee : getFeesToBeApportioned(feePayApportionCCDCase.getFees())) {
                    if(fee.getIsFullyApportioned().equalsIgnoreCase("N")){

                        fee.setCurrApportionAmount(fee.getAllocatedAmount() != null ? fee.getAllocatedAmount() : new BigDecimal(0));
                        fee.setNetAmount(fee.getNetAmount() != null ? fee.getNetAmount() : getFeeCalculatedNetAmount(fee, feePayApportionCCDCase.getRemissions()));
                        BigDecimal calculatedFeeAmount = getFeeCalculatedPendingAmount(fee);
                        feePayApportions.add(applyFeePayApportion(fee, payment, calculatedFeeAmount, remainingPaymentAmount));
                        remainingPaymentAmount = remainingPaymentAmount.subtract(calculatedFeeAmount);
                        //payment.setAmount(remainingPaymentAmount);
                        if(remainingPaymentAmount.doubleValue() > 0) {
                            continue;
                        } else {
                            break;
                        }
                    }
                }
                // End Fee Loop
                if(remainingPaymentAmount.doubleValue() > 0) {
                    callSurplusAmount = remainingPaymentAmount;
                    isSurplus = true;
                } else if(remainingPaymentAmount.doubleValue() < 0){
                    callShortfallAmount = remainingPaymentAmount;
                    isShortfall = true;
                } else {
                    isSurplus = false;
                    isShortfall = false;
                }
            }
            // End Payment Loop
            if(isShortfall) {
                findShortfallFee(feePayApportions, callShortfallAmount);
            }
            if(isSurplus) {
                findSurplusFee(feePayApportions, callSurplusAmount, feePayApportionCCDCase.getFees());
            }
            feePayApportions.stream().forEach(feePayApportion -> {
                feePayApportionRepository.save(feePayApportion);
            });
            feePayApportionCCDCase.getFees().stream()
                .filter(fee -> fee.getIsFullyApportioned() != null)
                .forEach(fee -> {
                    //paymentFeeRepository.save(paymentFeeRepository.findById(fee.getId()).get());
                    paymentFeeRepository.save(fee);
                });
        }
        return feePayApportionCCDCase;
    }

    private BigDecimal getFeeCalculatedNetAmount(PaymentFee fee, List<Remission> remissions) {
        fee.setCalculatedAmount(fee.getCalculatedAmount() != null ? fee.getCalculatedAmount() : getFeeCalculatedAmount(fee));
        if(! CollectionUtils.isEmpty(remissions)) {
            remissions.stream()
                .filter(remission -> remission.getFee().getId().equals(fee.getId()))
                .forEach(remission -> fee.setCalculatedAmount(fee.getCalculatedAmount().subtract(remission.getHwfAmount())));
        }
        return fee.getCalculatedAmount();
    }

    private BigDecimal getFeeCalculatedAmount(PaymentFee fee) {
        fee.setVolume(fee.getVolume() > 0 ? fee.getVolume() : 1);
        return fee.getFeeAmount() != null ? fee.getFeeAmount().multiply(new BigDecimal(fee.getVolume())) : new BigDecimal(0);
    }

    @Override
    @Transactional
    public void processApportion(Payment payment) {
        Optional<List<PaymentFee>> savedFees = paymentFeeRepository.findByCcdCaseNumber(payment.getCcdCaseNumber());
        if(savedFees.isPresent()){
            List<PaymentFee> sortedFees = savedFees.get()
                .stream()
                .sorted(Comparator.comparing(PaymentFee::getDateCreated))
                .collect(Collectors.toList());
            this.processFeePayApportion(FeePayApportionCCDCase.feePayApportionCCDCaseWith()
                .ccdCaseNo(payment.getCcdCaseNumber())
                .fees(sortedFees)
                .payments(Lists.newArrayList(payment))
                .build());
        }
    }

    private List<Payment> getPaymentsToBeApportioned(List<Payment> payments) {
        return payments.stream()
            .filter(payment -> payment.getDateCreated().after(parseDate(APPORTION_GO_LIVE_DATE)) ||
                                    payment.getDateCreated().equals(parseDate(APPORTION_GO_LIVE_DATE)))
            .collect(Collectors.toList());
    }

    private List<PaymentFee> getFeesToBeApportioned(List<PaymentFee> fees) {
        for(PaymentFee fee : fees) {
            if(fee.getIsFullyApportioned() == null){
                fee.setIsFullyApportioned("N");
            }
        }
        return fees.stream()
            .filter(fee -> fee.getDateCreated().after(parseDate(APPORTION_GO_LIVE_DATE)) ||
                fee.getDateCreated().equals(parseDate(APPORTION_GO_LIVE_DATE)))
            .collect(Collectors.toList());
    }

    private void findSurplusFee(List<FeePayApportion> feePayApportions, BigDecimal callSurplusAmount, List<PaymentFee> fees) {
        if(feePayApportions.size() == 0) {
            fees = fees.stream()
                .filter(fee -> fee.getIsFullyApportioned() != null)
                .sorted(Comparator.comparing(PaymentFee::getDateCreated))
                .collect(Collectors.toList());
            if(fees.size() > 0) {
                PaymentFee lastApportionedFee = (PaymentFee) fees.toArray()[fees.size() - 1];
                lastApportionedFee.setAllocatedAmount(lastApportionedFee.getAllocatedAmount().add(callSurplusAmount));
            }
        } else {
            FeePayApportion lastFeePayApportion = (FeePayApportion) feePayApportions.toArray()[feePayApportions.size() - 1];

            if(lastFeePayApportion.getCurrApportionAmount() != null && lastFeePayApportion.getCurrApportionAmount().doubleValue() != 0
                && lastFeePayApportion.getCurrApportionAmount().doubleValue() == lastFeePayApportion.getFeeAmount().doubleValue() ) {
                lastFeePayApportion.setCallSurplusAmount(callSurplusAmount);
                lastFeePayApportion.setAllocatedAmount(lastFeePayApportion.getAllocatedAmount().add(callSurplusAmount));
                for(PaymentFee fee : fees){
                    if(fee.getId().intValue() == lastFeePayApportion.getFeeId().intValue()){
                        fee.setAllocatedAmount(lastFeePayApportion.getAllocatedAmount());
                    }
                }
            }
        }
    }

    private void findShortfallFee(List<FeePayApportion> feePayApportions, BigDecimal callShortfallAmount) {
        FeePayApportion lastFeePayApportion = (FeePayApportion) feePayApportions.toArray()[feePayApportions.size() - 1];

        if(lastFeePayApportion.getCurrApportionAmount() != null && lastFeePayApportion.getCurrApportionAmount().doubleValue() != 0
            && lastFeePayApportion.getCurrApportionAmount().doubleValue() < lastFeePayApportion.getFeeAmount().doubleValue() ) {
            lastFeePayApportion.setCallShortFallAmount(callShortfallAmount);
        }
    }

    private FeePayApportion applyFeePayApportion (PaymentFee fee, Payment payment, BigDecimal calculatedFeeAmount, BigDecimal remainingPaymentAmount) {
        if(fee.getCurrApportionAmount().doubleValue() != fee.getNetAmount().doubleValue()) {
            if (remainingPaymentAmount.doubleValue() > calculatedFeeAmount.doubleValue()) {
                fee.setCurrApportionAmount(fee.getCurrApportionAmount().add(calculatedFeeAmount));
            } else {
                fee.setCurrApportionAmount(fee.getCurrApportionAmount().add(remainingPaymentAmount));
            }

            if(fee.getCurrApportionAmount().doubleValue() == fee.getNetAmount().doubleValue()){
                // Mark Fee as Fully Apportioned
                fee.setIsFullyApportioned("Y");
                resetShortFallAmount(fee);
            }

            // Create a new Record in FeePayApportion Table

            FeePayApportion feePayApportion = FeePayApportion.feePayApportionWith()
                .feeId(fee.getId())
                .paymentId(payment.getId())
                .feeAmount(fee.getNetAmount())
                .paymentAmount(payment.getAmount())
                .isFullyApportioned(fee.getIsFullyApportioned())
                .currApportionAmount(fee.getCurrApportionAmount())
                .ccdCaseNumber(payment.getCcdCaseNumber())
                .createdBy("SYSTEM")
                .dateCreated(payment.getDateCreated())
                .build();

            if (remainingPaymentAmount.doubleValue() > calculatedFeeAmount.doubleValue()) {
                feePayApportion.setApportionAmount(calculatedFeeAmount);
            } else {
                feePayApportion.setApportionAmount(remainingPaymentAmount);
            }

            feePayApportion.setAllocatedAmount(fee.getCurrApportionAmount());

            // Update FEE according to apportion allocation

            fee.setApportionAmount(feePayApportion.getCurrApportionAmount());
            fee.setAllocatedAmount(feePayApportion.getAllocatedAmount());
            fee.setDateApportioned(feePayApportion.getDateCreated());

            return feePayApportion;
        }
        return null;
    }

    private void resetShortFallAmount(PaymentFee fee) {
        this.isShortfall = false;
    }

    private BigDecimal getFeeCalculatedPendingAmount(PaymentFee fee) {
        return fee.getNetAmount().subtract(fee.getCurrApportionAmount());
    }

    private Date parseDate(String date) {
        try {
            return new SimpleDateFormat("dd.MM.yyyy").parse(date);
        } catch (ParseException e) {
            return null;
        }
    }
}
