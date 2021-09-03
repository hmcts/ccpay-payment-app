package uk.gov.hmcts.payment.api.service;

import com.google.common.collect.Lists;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.dto.RemissionServiceRequest;
import uk.gov.hmcts.payment.api.dto.RetroRemissionServiceRequest;
import uk.gov.hmcts.payment.api.model.FeePayApportion;
import uk.gov.hmcts.payment.api.model.FeePayApportionRepository;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.model.Remission;
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
import java.util.stream.Collectors;

@Service
public class RemissionServiceImpl implements RemissionService {

    private final PaymentFeeLinkRepository paymentFeeLinkRepository;
    private final FeePayApportionRepository feePayApportionRepository;
    private final Payment2Repository paymentRespository;
    private final ReferenceUtil referenceUtil;

    private final OrderCaseUtil orderCaseUtil;

    @Autowired
    public RemissionServiceImpl(PaymentFeeLinkRepository paymentFeeLinkRepository,
                                FeePayApportionRepository feePayApportionRepository, Payment2Repository paymentRespository, ReferenceUtil referenceUtil, OrderCaseUtil orderCaseUtil) {
        this.paymentFeeLinkRepository = paymentFeeLinkRepository;
        this.feePayApportionRepository = feePayApportionRepository;
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
    public Remission createRetrospectiveRemissionForPayment(RetroRemissionServiceRequest remissionServiceRequest, String paymentGroupReference, Integer feeId) throws CheckDigitException {
        PaymentFeeLink paymentFeeLink = populatePaymentFeeLink(paymentGroupReference);
        PaymentFee fee = populatePaymentFee(feeId, paymentFeeLink, remissionServiceRequest);
        FeePayApportion feePayApportion = populatePaymentApportionment(feeId);
        return buildRemissionForPayment(paymentFeeLink, fee, remissionServiceRequest);
    }

    private PaymentFeeLink populatePaymentFeeLink(String paymentGroupReference) {
        return paymentFeeLinkRepository.findByPaymentReference(paymentGroupReference)
            .orElseThrow(() -> new InvalidPaymentGroupReferenceException("Payment group " + paymentGroupReference + " does not exists."));
    }

    private PaymentFee populatePaymentFee(Integer feeId, PaymentFeeLink paymentFeeLink, RetroRemissionServiceRequest remissionServiceRequest) {
        // Get particular fee from paymentFeeLink using feeId
        PaymentFee fee = paymentFeeLink.getFees().stream().filter(f -> f.getId().equals(feeId))
            .findAny()
            .orElseThrow(() -> new PaymentFeeNotFoundException("Fee with id " + feeId + " does not exists."));
        if (!fee.getRemissions().isEmpty()) {
            throw new RemissionAlreadyExistException("Remission is already exist for FeeId " + feeId);
        } else if (fee.getCalculatedAmount().compareTo(remissionServiceRequest.getHwfAmount()) < 0) {
            throw new RemissionNotFoundException("Hwf Amount should not be more than Fee amount");
        }
        return fee;
    }

    private FeePayApportion populatePaymentApportionment(Integer feeId) {
        // If there are more than one payment for a Fee then not eligible for remission
        Optional<FeePayApportion> feePayApportion = feePayApportionRepository.findByFeeId(feeId);
        if (!feePayApportion.isPresent()) {
                throw new InvalidPaymentGroupReferenceException("This fee " + feeId + " is not found. Hence not eligible for remission");
        }
        return feePayApportion.get();
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


    private Remission buildRemissionForPayment(PaymentFeeLink paymentFeeLink, PaymentFee fee, RetroRemissionServiceRequest remissionServiceRequest) throws CheckDigitException {
        // Apply retro remission using all data from paymentFeeLink,fee,feePayApportion,remissionServiceRequest
        Remission remission = Remission.remissionWith()
            .hwfReference(remissionServiceRequest.getHwfReference())
            .hwfAmount(remissionServiceRequest.getHwfAmount())
            .remissionReference(referenceUtil.getNext("RM"))
            .siteId(paymentFeeLink.getOrgId())
            .ccdCaseNumber(fee.getCcdCaseNumber())
            .caseReference(paymentFeeLink.getCaseReference())
            .build();

        fee.setRemissions(Lists.newArrayList(remission));
        paymentFeeLink.setRemissions(Lists.newArrayList(remission));
        return remission;
    }
}
