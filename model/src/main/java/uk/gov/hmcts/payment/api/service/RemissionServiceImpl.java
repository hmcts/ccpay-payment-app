package uk.gov.hmcts.payment.api.service;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.dto.RemissionServiceRequest;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.model.Remission;
import uk.gov.hmcts.payment.api.model.RemissionRepository;
import uk.gov.hmcts.payment.api.util.ReferenceUtil;

import java.util.Collections;
import java.util.Optional;

@Service
public class RemissionServiceImpl implements RemissionService {

    private final PaymentFeeLinkRepository paymentFeeLinkRepository;
    private final RemissionRepository remissionRepository;
    private final ReferenceUtil referenceUtil;

    @Autowired
    public RemissionServiceImpl(PaymentFeeLinkRepository paymentFeeLinkRepository,
                                RemissionRepository remissionRepository,
                                ReferenceUtil referenceUtil) {
        this.paymentFeeLinkRepository = paymentFeeLinkRepository;
        this.remissionRepository = remissionRepository;
        this.referenceUtil = referenceUtil;
    }

    @Override
    public PaymentFeeLink create(RemissionServiceRequest remissionServiceRequest) throws CheckDigitException {
        String remissionReference = referenceUtil.getNext("RM");
        remissionServiceRequest.setRemissionReference(remissionReference);

        Remission remission = buildRemission(remissionServiceRequest);

        PaymentFeeLink paymentFeeLink = PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference(remissionServiceRequest.getPaymentGroupReference())
            .remissions(Collections.singletonList(remission))
            .fees(remissionServiceRequest.getFees())
            .build();
        remission.setPaymentFeeLink(paymentFeeLink);

        return paymentFeeLinkRepository.save(paymentFeeLink);

    }

    private Remission buildRemission(RemissionServiceRequest remissionServiceRequest) {
        return Remission.remissionWith()
            .remissionReference(remissionServiceRequest.getRemissionReference())
            .hwfReference(remissionServiceRequest.getHwfReference())
            .hwfAmount(remissionServiceRequest.getHwfAmount())
            .beneficiaryName(remissionServiceRequest.getBeneficiaryName())
            .ccdCaseNumber(remissionServiceRequest.getCcdCaseNumber())
            .caseReference(remissionServiceRequest.getCaseReference())
            .build();
    }

    @Override
    public Remission retrieve(String hwfReference) {
        Optional<Remission> remission = remissionRepository.findByHwfReference(hwfReference);
        return remission.orElse(null);
    }
}
