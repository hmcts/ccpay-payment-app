package uk.gov.hmcts.payment.api.service;

import com.google.common.collect.Lists;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.dto.RemissionServiceRequest;
import uk.gov.hmcts.payment.api.dto.RetroRemissionServiceRequest;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.util.ReferenceUtil;
import uk.gov.hmcts.payment.api.v1.model.exceptions.InvalidPaymentGroupReferenceException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentFeeNotFoundException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.RemissionAlreadyExistException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.RemissionNotFoundException;

import javax.transaction.Transactional;
import java.util.Collections;

@Service
public class RemissionServiceImpl implements RemissionService {

    private final PaymentFeeLinkRepository paymentFeeLinkRepository;
    private final FeePayApportionRepository feePayApportionRepository;
    private final PaymentFeeRepository paymentFeeRepository;
    private final ReferenceUtil referenceUtil;

    @Autowired
    public RemissionServiceImpl(PaymentFeeLinkRepository paymentFeeLinkRepository,
                                FeePayApportionRepository feePayApportionRepository, PaymentFeeRepository paymentFeeRepository, ReferenceUtil referenceUtil) {
        this.paymentFeeLinkRepository = paymentFeeLinkRepository;
        this.feePayApportionRepository = feePayApportionRepository;
        this.paymentFeeRepository = paymentFeeRepository;
        this.referenceUtil = referenceUtil;
    }

    @Override
    public PaymentFeeLink createRemission(RemissionServiceRequest remissionServiceRequest) throws CheckDigitException {
        String remissionReference = referenceUtil.getNext("RM");
        remissionServiceRequest.setRemissionReference(remissionReference);

        Remission remission = buildRemission(remissionServiceRequest);
        PaymentFee fee = remissionServiceRequest.getFee();

        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference(remissionServiceRequest.getPaymentGroupReference())
            .remissions(Collections.singletonList(remission))
            .fees(Collections.singletonList(fee))
            .build();
        remission.setPaymentFeeLink(paymentFeeLink);
        fee.setRemissions(Collections.singletonList(remission));

        return paymentFeeLinkRepository.save(paymentFeeLink);

    }

    @Override
    @Transactional
    public PaymentFeeLink createRetrospectiveRemission(RemissionServiceRequest remissionServiceRequest, String paymentGroupReference, Integer feeId) throws CheckDigitException {
        PaymentFeeLink paymentFeeLink = paymentFeeLinkRepository.findByPaymentReference(paymentGroupReference)
            .orElseThrow(() -> new InvalidPaymentGroupReferenceException("Payment group " + paymentGroupReference + " does not exists."));

        // Tactical check where feeId is null
        PaymentFee fee = feeId != null ? paymentFeeLink.getFees().stream().filter(f -> f.getId().equals(feeId))
            .findAny()
            .orElseThrow(() -> new PaymentFeeNotFoundException("Fee with id " + feeId + " does not exists.")) :
            paymentFeeLink.getFees().stream().filter(f -> f.getCode().equals(remissionServiceRequest.getFee().getCode()))
                .findAny()
                .orElseThrow(() -> new PaymentFeeNotFoundException("Fee with code " + remissionServiceRequest.getFee().getCode() + " does not exists."));

        if (!fee.getRemissions().isEmpty()) {
            throw new RemissionAlreadyExistException("Remission is already applied to the Fee "+fee.getCode());
        }

        String remissionReference = referenceUtil.getNext("RM");
        remissionServiceRequest.setRemissionReference(remissionReference);

        Remission remission = buildRemission(remissionServiceRequest);

        paymentFeeLink.setRemissions(Lists.newArrayList(remission));

        fee.setNetAmount(fee.getCalculatedAmount().subtract(remission.getHwfAmount()));

        if (fee.getRemissions() == null) {
            fee.setRemissions(Lists.newArrayList(remission));
        } else {
            fee.getRemissions().add(remission);
        }

        remission.setPaymentFeeLink(paymentFeeLink);
        remission.setFee(fee);

        return paymentFeeLink;
    }

    @Override
    @Transactional
    public Remission  createRetrospectiveRemissionForPayment(RetroRemissionServiceRequest remissionServiceRequest, String paymentGroupReference, Integer feeId) throws CheckDigitException {

        /* // Need to implement using FeePayApportion
        List<FeePayApportion> feePayApportion = feePayApportionRepository.findByFeeId(feeId)
            .orElseThrow(() -> new InvalidPaymentGroupReferenceException("Cannot find apportionment entry with feeId "+feeId));

        PaymentFee fee = paymentFeeRepository.findById(feePayApportion.get(0).getFeeId())
            .orElseThrow(() -> new InvalidPaymentGroupReferenceException("Fee with id " + feeId + " does not exists."));

        Remission remission = fee.getRemissions().stream().filter(r->r.getHwfReference().equalsIgnoreCase(remissionServiceRequest.getHwfReference()))
            .findAny()
            .orElseThrow(() -> new RemissionNotFoundException("No remission found for reference "+remissionServiceRequest.getHwfReference()));

        feePayApportion.getPayments().stream().forEach(p-> {
                if (p.getPaymentStatus().getName().equalsIgnoreCase("success")) {
                    fee.setAmountDue(fee.getCalculatedAmount().subtract(remission.getHwfAmount()).subtract(p.getAmount()));
                } else {
                    fee.setAmountDue(fee.getCalculatedAmount().subtract(remission.getHwfAmount()));
                }
            }*/

       PaymentFeeLink paymentFeeLink = paymentFeeLinkRepository.findByPaymentReference(paymentGroupReference)
            .orElseThrow(() -> new InvalidPaymentGroupReferenceException("Payment group " + paymentGroupReference + " does not exists."));

       PaymentFee fee =  paymentFeeLink.getFees().stream().filter(f -> f.getId().equals(feeId))
            .findAny()
            .orElseThrow(() -> new PaymentFeeNotFoundException("Fee with id " + feeId + " does not exists.")) ;

       Remission remission = fee.getRemissions().stream().filter(r->r.getHwfReference().equalsIgnoreCase(remissionServiceRequest.getHwfReference()))
            .findAny()
            .orElseThrow(() -> new RemissionNotFoundException("No remission found for reference "+remissionServiceRequest.getHwfReference()));

            paymentFeeLink.getPayments().stream().forEach(p-> {
                if(p.getPaymentStatus().getName().equalsIgnoreCase("success")){
                    fee.setAmountDue(fee.getCalculatedAmount().subtract(remission.getHwfAmount()).subtract(p.getAmount()));
                }else{
                    fee.setAmountDue(fee.getCalculatedAmount().subtract(remission.getHwfAmount()));
                }
            });

        return remission;
    }

    private Remission buildRemission(RemissionServiceRequest remissionServiceRequest) {
        return Remission.remissionWith()
            .remissionReference(remissionServiceRequest.getRemissionReference())
            .hwfReference(remissionServiceRequest.getHwfReference())
            .hwfAmount(remissionServiceRequest.getHwfAmount())
            .beneficiaryName(remissionServiceRequest.getBeneficiaryName())
            .ccdCaseNumber(remissionServiceRequest.getCcdCaseNumber())
            .caseReference(remissionServiceRequest.getCaseReference())
            .siteId(remissionServiceRequest.getSiteId())
            .build();
    }

}
