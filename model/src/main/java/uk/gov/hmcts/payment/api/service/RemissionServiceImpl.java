package uk.gov.hmcts.payment.api.service;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.dto.RemissionServiceRequest;
import uk.gov.hmcts.payment.api.dto.RetroRemissionServiceRequest;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.model.Remission;
import uk.gov.hmcts.payment.api.util.ReferenceUtil;
import uk.gov.hmcts.payment.api.util.ServiceRequestCaseUtil;
import uk.gov.hmcts.payment.api.v1.model.exceptions.InvalidPaymentGroupReferenceException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentFeeNotFoundException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.RemissionAlreadyExistException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.RemissionNotFoundException;

import javax.transaction.Transactional;
import java.util.Collections;
import java.util.Optional;

@Service
@Slf4j
public class RemissionServiceImpl implements RemissionService {

    private final PaymentFeeLinkRepository paymentFeeLinkRepository;
    private final ReferenceUtil referenceUtil;

    private final ServiceRequestCaseUtil serviceRequestCaseUtil;

    private static final String MESSAGE = " does not exists.";

    @Autowired
    public RemissionServiceImpl(PaymentFeeLinkRepository paymentFeeLinkRepository,
                                ReferenceUtil referenceUtil, ServiceRequestCaseUtil serviceRequestCaseUtil) {
        this.paymentFeeLinkRepository = paymentFeeLinkRepository;
        this.referenceUtil = referenceUtil;
        this.serviceRequestCaseUtil = serviceRequestCaseUtil;
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

        return paymentFeeLinkRepository.save(serviceRequestCaseUtil.enhanceWithServiceRequestCaseDetails(paymentFeeLink, remissionServiceRequest));

    }

    @Override
    @Transactional
    public PaymentFeeLink createRetrospectiveRemission(RemissionServiceRequest remissionServiceRequest, String paymentGroupReference, Integer feeId) throws CheckDigitException {
        PaymentFeeLink paymentFeeLink =
            paymentFeeLinkRepository.findByPaymentReference(paymentGroupReference)
                .orElseGet(() -> paymentFeeLinkRepository.findByPaymentReferenceAndCcdCaseNumber(paymentGroupReference, remissionServiceRequest.getCcdCaseNumber())
                    .orElseThrow(() -> new InvalidPaymentGroupReferenceException("Payment group " + paymentGroupReference + MESSAGE)));

        serviceRequestCaseUtil.updateServiceRequestCaseDetails(paymentFeeLink, remissionServiceRequest);

        // Tactical check where feeId is null
        PaymentFee fee = feeId != null ? paymentFeeLink.getFees().stream().filter(f -> f.getId().equals(feeId))
            .findAny()
            .orElseThrow(() -> new PaymentFeeNotFoundException("Fee with id " + feeId + MESSAGE)) :
            paymentFeeLink.getFees().stream().filter(f -> f.getCode().equals(remissionServiceRequest.getFee().getCode()))
                .findAny()
                .orElseThrow(() -> new PaymentFeeNotFoundException("Fee with code " + remissionServiceRequest.getFee().getCode() + MESSAGE));

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
        return buildRemissionForPayment(paymentFeeLink, fee, remissionServiceRequest);
    }

    private PaymentFeeLink populatePaymentFeeLink(String paymentGroupReference) {
        return paymentFeeLinkRepository.findByPaymentReference(paymentGroupReference)
            .orElseThrow(() -> new InvalidPaymentGroupReferenceException("Payment group " + paymentGroupReference + MESSAGE));
    }

    private PaymentFee populatePaymentFee(Integer feeId, PaymentFeeLink paymentFeeLink, RetroRemissionServiceRequest remissionServiceRequest) {
        // Get particular fee from paymentFeeLink using feeId
        PaymentFee fee = paymentFeeLink.getFees().stream().filter(f -> f.getId().equals(feeId))
            .findAny()
            .orElseThrow(() -> new PaymentFeeNotFoundException("Fee with id " + feeId + MESSAGE));
        if (!fee.getRemissions().isEmpty()) {
            throw new RemissionAlreadyExistException("Remission is already exist for FeeId " + feeId);
        } else if (fee.getCalculatedAmount().compareTo(remissionServiceRequest.getHwfAmount()) < 0) {
            throw new RemissionNotFoundException("Hwf Amount should not be more than Fee amount");
        }
        return fee;
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
