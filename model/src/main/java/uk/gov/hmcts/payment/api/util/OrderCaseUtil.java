package uk.gov.hmcts.payment.api.util;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.dto.RemissionServiceRequest;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

@Component
public class OrderCaseUtil {

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

//        if (payment.getCcdCaseNumber() != null) {
//            Optional<CaseDetails> existingCaseDetails = caseDetailsRepository.findByCcdCaseNumber(payment.getCcdCaseNumber());
//            if (existingCaseDetails.isPresent()) {
//                paymentFeeLink.setCaseDetails(Collections.singleton(existingCaseDetails.get()));
//            } else {
//                paymentFeeLink.setCaseDetails(Collections.singleton(CaseDetails.caseDetailsWith()
//                    .ccdCaseNumber(payment.getCcdCaseNumber())
//                    .caseReference(payment.getCaseReference())
//                    .build()));
//            }
//        }

        paymentFeeLink.setEnterpriseServiceName(payment.getServiceType());
        paymentFeeLink.setOrgId(payment.getSiteId());
        paymentFeeLink.setCcdCaseNumber(payment.getCcdCaseNumber());
        paymentFeeLink.setCaseReference(payment.getCaseReference());

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
