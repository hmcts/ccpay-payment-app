package uk.gov.hmcts.payment.api.jpaaudit.listner;

import uk.gov.hmcts.payment.api.jpaaudit.model.AuditEventsType;
import uk.gov.hmcts.payment.api.jpaaudit.model.PaymentAuditHistory;
import uk.gov.hmcts.payment.api.model.Remission;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PostPersist;
import jakarta.transaction.Transactional;

import static jakarta.transaction.Transactional.TxType.MANDATORY;
import static uk.gov.hmcts.payment.api.jpaaudit.listner.Action.INSERTED;
import static uk.gov.hmcts.payment.api.jpaaudit.model.AuditEventsType.REMISSION_APPLIED;

public class RemissionEntityListener {

    @PostPersist
    public void postPersist(Remission target) { perform(target, INSERTED, REMISSION_APPLIED); }

    @Transactional(MANDATORY)
    private void perform(Remission target, Action action, AuditEventsType auditEventsType) {
        EntityManager entityManager = BeanUtil.getBean(EntityManager.class);
        entityManager.persist(PaymentAuditHistory.paymentAuditHistoryWith()
            .ccdCaseNo(target.getCcdCaseNumber())
            .auditType(auditEventsType)
            .auditPayload(target.toString())
            .auditDescription(auditEventsType.getDescription())
            .build());
    }
}
