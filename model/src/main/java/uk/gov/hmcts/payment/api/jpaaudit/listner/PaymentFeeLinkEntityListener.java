package uk.gov.hmcts.payment.api.jpaaudit.listner;

import uk.gov.hmcts.payment.api.jpaaudit.model.AuditEventsType;
import uk.gov.hmcts.payment.api.jpaaudit.model.PaymentAuditHistory;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PostPersist;
import jakarta.transaction.Transactional;

import static jakarta.transaction.Transactional.TxType.MANDATORY;
import static uk.gov.hmcts.payment.api.jpaaudit.listner.Action.INSERTED;
import static uk.gov.hmcts.payment.api.jpaaudit.model.AuditEventsType.ORDER_CREATED;

public class PaymentFeeLinkEntityListener {

    @PostPersist()
    public void postPersist(PaymentFeeLink target) {
        perform(target, INSERTED, ORDER_CREATED);
    }

    @Transactional(MANDATORY)
    private void perform(PaymentFeeLink target, Action action, AuditEventsType auditEventsType) {
        EntityManager entityManager = BeanUtil.getBean(EntityManager.class);

//        String ccdCaseNumber =target.getCaseDetails().iterator().next().getCcdCaseNumber();
//        Will be used once all services onboarded orders endpoint
        entityManager.persist(PaymentAuditHistory.paymentAuditHistoryWith()
            .ccdCaseNo(target.getCcdCaseNumber())
            .auditType(auditEventsType)
            .auditPayload(target.toString())
            .auditDescription(auditEventsType.getDescription())
            .build());
    }
}
