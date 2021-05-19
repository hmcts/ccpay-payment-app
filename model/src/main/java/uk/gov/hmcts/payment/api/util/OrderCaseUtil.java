package uk.gov.hmcts.payment.api.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.dto.RemissionServiceRequest;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;

@Component
public class OrderCaseUtil {

    @Autowired
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    public void updateOrderCaseDetails(PaymentFeeLink paymentFeeLink, Payment payment) {

//        Add null chekcer for paymentfeelink

//        if (paymentFeeLink.getCaseDetails().isEmpty() && payment.getCcdCaseNumber() != null) {
//            paymentFeeLink.setCaseDetails(Collections.singleton(CaseDetails.caseDetailsWith()
//                .ccdCaseNumber(payment.getCcdCaseNumber())
//                .caseReference(payment.getCaseReference())
//                .build()));
//        } else {
//            paymentFeeLink.getCaseDetails().stream().forEach(caseDetails -> {
//                caseDetails.setCcdCaseNumber(payment.getCcdCaseNumber());
//            });
//        }
        if (payment.getCcdCaseNumber() != null) {
            paymentFeeLink.setCcdCaseNumber(payment.getCcdCaseNumber());
        }
        if (payment.getCaseReference() != null) {
            paymentFeeLink.setCaseReference(payment.getCaseReference());
        }
        paymentFeeLink.setEnterpriseServiceName(payment.getServiceType());
        paymentFeeLink.setOrgId(payment.getSiteId());
    }

    public void updateOrderCaseDetails(PaymentFeeLink paymentFeeLink, RemissionServiceRequest remission) {
        if (remission.getCcdCaseNumber() != null) {
            paymentFeeLink.setCcdCaseNumber(remission.getCcdCaseNumber());
            paymentFeeLink.setCaseReference(remission.getCaseReference());
        }
        paymentFeeLink.setOrgId(remission.getSiteId());
    }

    public PaymentFeeLink enhanceWithOrderCaseDetails(PaymentFeeLink paymentFeeLink, Payment payment) {

        paymentFeeLink.setOrgId(payment.getSiteId());
        paymentFeeLink.setEnterpriseServiceName(payment.getServiceType());
        if (payment.getCcdCaseNumber() != null) {
            paymentFeeLink.setCcdCaseNumber(payment.getCcdCaseNumber());
        }
        if (payment.getCaseReference() != null) {
            paymentFeeLink.setCaseReference(payment.getCaseReference());
        }
        return paymentFeeLink;
    }

    public PaymentFeeLink enhanceWithOrderCaseDetails(PaymentFeeLink paymentFeeLink, RemissionServiceRequest remission) {

//        if (remission.getCcdCaseNumber() != null) {
//            Optional<CaseDetails> existingCaseDetails = caseDetailsRepository.findByCcdCaseNumber(remission.getCcdCaseNumber());
//            if (existingCaseDetails.isPresent()) {
//                paymentFeeLink.setCaseDetails(Collections.singleton(existingCaseDetails.get()));
//            } else {
//                paymentFeeLink.setCaseDetails(Collections.singleton(CaseDetails.caseDetailsWith()
//                    .ccdCaseNumber(remission.getCcdCaseNumber())
//                    .caseReference(remission.getCaseReference())
//                    .build()));
//            }
//        }

        paymentFeeLink.setOrgId(remission.getSiteId());
        if (remission.getCcdCaseNumber() != null) {
            paymentFeeLink.setCcdCaseNumber(remission.getCcdCaseNumber());
            paymentFeeLink.setCaseReference(remission.getCaseReference());
        }
        return paymentFeeLink;
    }
}
