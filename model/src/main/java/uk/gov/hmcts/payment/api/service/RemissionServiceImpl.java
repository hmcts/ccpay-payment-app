package uk.gov.hmcts.payment.api.service;

import com.google.common.collect.Lists;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.dto.RemissionServiceRequest;
import uk.gov.hmcts.payment.api.dto.RetroRemissionServiceRequest;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.util.OrderCaseUtil;
import uk.gov.hmcts.payment.api.util.ReferenceUtil;
import uk.gov.hmcts.payment.api.v1.model.exceptions.InvalidPaymentGroupReferenceException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentFeeNotFoundException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.RemissionAlreadyExistException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.RemissionNotFoundException;

import javax.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class RemissionServiceImpl implements RemissionService {

    private final PaymentFeeLinkRepository paymentFeeLinkRepository;
    private final FeePayApportionRepository feePayApportionRepository;
    private final PaymentFeeRepository paymentFeeRepository;
    private final Payment2Repository paymentRespository;
    private final ReferenceUtil referenceUtil;

    private final OrderCaseUtil orderCaseUtil;

    @Autowired
    public RemissionServiceImpl(PaymentFeeLinkRepository paymentFeeLinkRepository,
                                FeePayApportionRepository feePayApportionRepository, PaymentFeeRepository paymentFeeRepository, Payment2Repository paymentRespository, ReferenceUtil referenceUtil, OrderCaseUtil orderCaseUtil) {
        this.paymentFeeLinkRepository = paymentFeeLinkRepository;
        this.feePayApportionRepository = feePayApportionRepository;
        this.paymentFeeRepository = paymentFeeRepository;
        this.paymentRespository = paymentRespository;
        this.referenceUtil = referenceUtil;
        this.orderCaseUtil = orderCaseUtil;
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

        return paymentFeeLinkRepository.save(orderCaseUtil.enhanceWithOrderCaseDetails(paymentFeeLink, remissionServiceRequest));

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

        String remissionReference = referenceUtil.getNext("RM");
        remissionServiceRequest.setRemissionReference(remissionReference);

        Remission remission = buildRemission(remissionServiceRequest);

        fee.setNetAmount(fee.getCalculatedAmount().subtract(remission.getHwfAmount()));

        if (fee.getRemissions()==null || fee.getRemissions().isEmpty()) {
            paymentFeeLink.setRemissions(Lists.newArrayList(remission));
            fee.setRemissions(Lists.newArrayList(remission));
        } else {
            throw new RemissionAlreadyExistException("Remission is already applied to the Fee "+fee.getCode());
        }
        remission.setPaymentFeeLink(paymentFeeLink);
        remission.setFee(fee);

        return paymentFeeLink;
    }

    @Override
    @Transactional
    public Remission  createRetrospectiveRemissionForPayment(RetroRemissionServiceRequest remissionServiceRequest, String paymentGroupReference, Integer feeId) throws CheckDigitException {

        List<FeePayApportion> feePayApportion = feePayApportionRepository.findByFeeId(feeId)
            .orElseThrow(() -> new InvalidPaymentGroupReferenceException("Cannot find apportionment entry with feeId "+feeId));

        PaymentFee fee = paymentFeeRepository.findById(feeId)
            .orElseThrow(() -> new PaymentFeeNotFoundException("Fee with id " + feeId + " does not exists."));

        Optional<Payment> payment = null;
        if(!feePayApportion.isEmpty()&&feePayApportion.size()==1){
            payment = paymentRespository.findById(feePayApportion.get(0).getPaymentId());
        } else{
            throw new InvalidPaymentGroupReferenceException("Multiple payments or No Payment for apportionment for " + feeId + " fee id.");
        }

       Remission remission = fee.getRemissions().stream().filter(r->r.getHwfReference().equalsIgnoreCase(remissionServiceRequest.getHwfReference()))
            .findAny()
            .orElseThrow(() -> new RemissionNotFoundException("No remission found for reference "+remissionServiceRequest.getHwfReference()));

                if(payment.get().getPaymentMethod().getName().equalsIgnoreCase("payment by account") && payment.get().getPaymentStatus().getName().equalsIgnoreCase("success")){
                    fee.setAmountDue(fee.getCalculatedAmount().subtract(remission.getHwfAmount()).subtract(payment.get().getAmount()));
                }else{
                    fee.setAmountDue(fee.getCalculatedAmount().subtract(remission.getHwfAmount()));
                }
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
