package uk.gov.hmcts.payment.api.service;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class FeePayApportionServiceImpl implements FeePayApportionService {

    private static final Logger LOG = LoggerFactory.getLogger(FeePayApportionServiceImpl.class);

    private final PaymentFeeLinkRepository paymentFeeLinkRepository;

    private final PaymentStatusRepository paymentStatusRepository;

    private final FeePayStagedRepository feePayStagedRepository;

    private final FeePayApportionRepository feePayApportionRepository;

    private final PaymentFeeRepository paymentFeeRepository;

    private final String APPORTION_GO_LIVE_DATE = "01.08.2020";

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
    public List<FeePayApportionCCDCase> findAllHistoricalCases() {

        List<FeePayApportionCCDCase> feePayApportionCCDCases = new ArrayList<>();

        List<FeePayStaged> allFeePayStagedData = (List<FeePayStaged>) feePayStagedRepository.findAll();

        if(CollectionUtils.isEmpty(allFeePayStagedData)) {
            LOG.info("No Staged Data Found for FeePay Apportion!!!");
        } else {
            allFeePayStagedData.stream()
                .filter(distinctByKey(FeePayStaged::getCcdCaseNo))
                .collect(Collectors.toList())
                .forEach(feePayStaged -> {
                    feePayApportionCCDCases.add(FeePayApportionCCDCase.feePayApportionCCDCaseWith().ccdCaseNo(feePayStaged.getCcdCaseNo()).build());
                });
        }

        feePayApportionCCDCases.stream().forEach(feePayApportionCCDCase -> {

            feePayApportionCCDCase.setFeePayGroups(new ArrayList<>());
            feePayApportionCCDCase.setFees(new ArrayList<>());
            feePayApportionCCDCase.setRemissions(new ArrayList<>());
            feePayApportionCCDCase.setPayments(new ArrayList<>());

            List<FeePayStaged> feePayStagedByCase = allFeePayStagedData.stream()
                .filter(feePayStaged -> feePayStaged.getCcdCaseNo().equalsIgnoreCase(feePayApportionCCDCase.getCcdCaseNo()))
                .collect(Collectors.toList());

            feePayStagedByCase.stream()
                .filter(distinctByKey(FeePayStaged::getGroupReference))
                .collect(Collectors.toList())
                .forEach(feePayStaged -> {
                    feePayApportionCCDCase.getFeePayGroups().add(PaymentFeeLink.paymentFeeLinkWith().paymentReference(feePayStaged.getGroupReference()).build());
                });

            feePayStagedByCase.stream()
                .filter(distinctByKey(FeePayStaged::getFeeId))
                .collect(Collectors.toList())
                .forEach(feePayStaged -> {
                    feePayApportionCCDCase.getFees().add(PaymentFee.feeWith()
                        .id(feePayStaged.getFeeId())
                        .code(feePayStaged.getFeeCode())
                        .feeAmount(feePayStaged.getFeeAmount())
                        .volume(feePayStaged.getVolume())
                        .calculatedAmount(feePayStaged.getCalculatedAmount())
                        .netAmount(feePayStaged.getNetAmount() != null ? feePayStaged.getNetAmount() : feePayStaged.getCalculatedAmount())
                        .currApportionAmount(new BigDecimal(0))
                        .dateCreated(feePayStaged.getFeeCreatedDate())
                        .build());
                    /*if(feePayStaged.getHwfAmount() != null && feePayStaged.getHwfAmount().doubleValue() > 0) {
                        feePayApportionCCDCase.getRemissions().add(Remission.remissionWith()
                            .hwfAmount(feePayStaged.getHwfAmount())
                            .build());
                    }*/
                });

            //Sorting Fees for a Case in Date Created Ascending order
            feePayApportionCCDCase.setFees(feePayApportionCCDCase.getFees().stream()
                .sorted(Comparator.comparing(PaymentFee::getDateCreated))
                .collect(Collectors.toList()));

            feePayStagedByCase.stream()
                .filter(distinctByKey(FeePayStaged::getPaymentId))
                .collect(Collectors.toList())
                .forEach(feePayStaged -> {
                    feePayApportionCCDCase.getPayments().add(Payment.paymentWith()
                        .id(feePayStaged.getPaymentId())
                        .amount(feePayStaged.getPaymentAmount())
                        .reference(feePayStaged.getPaymentRef())
                        .paymentStatus(PaymentStatus.paymentStatusWith().name(feePayStaged.getPaymentStatus()).build())
                        .dateCreated(feePayStaged.getPaymentCreatedDate())
                        .paymentChannel(PaymentChannel.paymentChannelWith().name(feePayStaged.getPaymentChannel()).build())
                        .paymentMethod(PaymentMethod.paymentMethodWith().name(feePayStaged.getPaymentMethod()).build())
                        .paymentProvider(PaymentProvider.paymentProviderWith().name(feePayStaged.getPaymentProvider()).build())
                        .serviceType(feePayStaged.getServiceType())
                        .ccdCaseNumber(feePayStaged.getCcdCaseNo())
                        .build());
                });

            //Sorting Payments for a Case in Date Created Ascending order
            feePayApportionCCDCase.setPayments(feePayApportionCCDCase.getPayments().stream()
                .sorted(Comparator.comparing(Payment::getDateCreated))
                .collect(Collectors.toList()));
        });
        return feePayApportionCCDCases;
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    @Override
    public FeePayApportion findFeePayGroupsByCase(String ccdCase) {
        return null;
    }

    @Override
    public FeePayApportion findFeesByFeePayGroup(PaymentFeeLink feePayGroup) {
        return null;
    }

    @Override
    public FeePayApportion findRemissionsByFeePayGroup(PaymentFeeLink feePayGroup) {
        return null;
    }

    @Override
    public FeePayApportion findPaymentsByFeePayGroup(PaymentFeeLink feePayGroup) {
        return null;
    }

    @Override
    @Transactional
    public FeePayApportionCCDCase processFeePayApportion(FeePayApportionCCDCase feePayApportionCCDCase) {

        BigDecimal callShortfallAmount = new BigDecimal(0);
        BigDecimal callSurplusAmount = new BigDecimal(0);
        BigDecimal remainingPaymentAmount = new BigDecimal(0);

        List<FeePayApportion> feePayApportions = new ArrayList<>();

        for(Payment payment : getPaymentsToBeApportioned(feePayApportionCCDCase.getPayments())) {
            remainingPaymentAmount = payment.getAmount();

             for(PaymentFee fee : getFeesToBeApportioned(feePayApportionCCDCase.getFees())) {
                if(fee.getIsFullyApportioned() == null){
                    fee.setIsFullyApportioned("N");
                }
                if(fee.getIsFullyApportioned().equalsIgnoreCase("N")){
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
        return feePayApportionCCDCase;
    }

    private List<Payment> getPaymentsToBeApportioned(List<Payment> payments) {
        return payments.stream()
            .filter(payment -> payment.getDateCreated().after(parseDate(APPORTION_GO_LIVE_DATE)) ||
                                    payment.getDateCreated().equals(parseDate(APPORTION_GO_LIVE_DATE)))
            .collect(Collectors.toList());
    }

    private List<PaymentFee> getFeesToBeApportioned(List<PaymentFee> fees) {
        return fees.stream()
            .filter(fee -> fee.getDateCreated().after(parseDate(APPORTION_GO_LIVE_DATE)) ||
                fee.getDateCreated().equals(parseDate(APPORTION_GO_LIVE_DATE)))
            .collect(Collectors.toList());
    }

    private void findSurplusFee(List<FeePayApportion> feePayApportions, BigDecimal callSurplusAmount, List<PaymentFee> fees) {
        FeePayApportion lastFeePayApportion = (FeePayApportion) feePayApportions.toArray()[feePayApportions.size() - 1];

        if(lastFeePayApportion.getCurrApportionAmount() != null && lastFeePayApportion.getCurrApportionAmount().doubleValue() != 0
            && lastFeePayApportion.getCurrApportionAmount().doubleValue() == lastFeePayApportion.getFeeAmount().doubleValue() ) {
            lastFeePayApportion.setCallSurplusAmount(callSurplusAmount);
            lastFeePayApportion.setAllocatedAmount(lastFeePayApportion.getAllocatedAmount().add(callSurplusAmount));
            for(PaymentFee fee : fees){
                if(fee.getId().intValue() == lastFeePayApportion.getFee().getId().intValue()){
                    fee.setAllocatedAmount(lastFeePayApportion.getAllocatedAmount());
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

    @Transactional
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
                .fee(fee)
                .payment(payment)
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

            fee.setAllocatedAmount(feePayApportion.getAllocatedAmount());
            fee.setDateApportioned(feePayApportion.getDateCreated());

            //if (feePayApportion.getApportionAmount().doubleValue() > 0) {
                System.out.println("Fee[" + fee.getId() + "] Amount ---> " + fee.getNetAmount() + " Payment[" + payment.getId() + "] Amount ---> " + payment.getAmount() + " Apportion Amount ---> " + feePayApportion.getApportionAmount());
                //feePayApportionRepository.save(feePayApportion);
            //}
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
