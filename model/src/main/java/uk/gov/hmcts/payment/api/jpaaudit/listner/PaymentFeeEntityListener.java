package uk.gov.hmcts.payment.api.jpaaudit.listner;

import uk.gov.hmcts.payment.api.jpaaudit.model.AuditEventsType;
import uk.gov.hmcts.payment.api.jpaaudit.model.PaymentAuditHistory;
import uk.gov.hmcts.payment.api.model.PaymentFee;

import javax.persistence.EntityManager;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.transaction.Transactional;

import static javax.transaction.Transactional.TxType.MANDATORY;
import static uk.gov.hmcts.payment.api.jpaaudit.listner.Action.INSERTED;
import static uk.gov.hmcts.payment.api.jpaaudit.listner.Action.UPDATED;
import static uk.gov.hmcts.payment.api.jpaaudit.model.AuditEventsType.FEE_CREATED;
import static uk.gov.hmcts.payment.api.jpaaudit.model.AuditEventsType.FEE_UPDATED;

public class PaymentFeeEntityListener {

    @PrePersist
    public void prePersist(PaymentFee target) { perform(target, INSERTED, FEE_CREATED); }

    @PreUpdate
    public void preUpdate(PaymentFee target) {
        perform(target, UPDATED, FEE_UPDATED);
    }

    @Transactional(MANDATORY)
    private void perform(PaymentFee target, Action action, AuditEventsType auditEventsType) {
        EntityManager entityManager = BeanUtil.getBean(EntityManager.class);
        entityManager.persist(PaymentAuditHistory.paymentAuditHistoryWith()
            .auditType(auditEventsType.getName())
            .auditPayload(target.getCode() + " | " + target.getCalculatedAmount() + " | " + target.getAllocatedAmount() + " | " + target.getDateCreated())
            .auditDescription(auditEventsType.getDescription())
            .build());
    }
}
