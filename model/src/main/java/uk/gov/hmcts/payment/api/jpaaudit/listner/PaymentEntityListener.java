package uk.gov.hmcts.payment.api.jpaaudit.listner;

import uk.gov.hmcts.payment.api.jpaaudit.model.AuditEventsType;
import uk.gov.hmcts.payment.api.jpaaudit.model.PaymentAuditHistory;
import uk.gov.hmcts.payment.api.model.Payment;

import javax.persistence.EntityManager;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.transaction.Transactional;

import static javax.transaction.Transactional.TxType.MANDATORY;
import static uk.gov.hmcts.payment.api.jpaaudit.listner.Action.INSERTED;
import static uk.gov.hmcts.payment.api.jpaaudit.listner.Action.UPDATED;
import static uk.gov.hmcts.payment.api.jpaaudit.model.AuditEventsType.PAYMENT_CREATED;
import static uk.gov.hmcts.payment.api.jpaaudit.model.AuditEventsType.PAYMENT_UPDATED;

public class PaymentEntityListener {

    @PrePersist
    public void prePersist(Payment target) {
        perform(target, INSERTED, PAYMENT_CREATED);
    }

    @PreUpdate
    public void preUpdate(Payment target) {
        perform(target, UPDATED, PAYMENT_UPDATED);
    }

    @Transactional(MANDATORY)
    private void perform(Payment target, Action action, AuditEventsType auditEventsType) {
        EntityManager entityManager = BeanUtil.getBean(EntityManager.class);
        entityManager.persist(PaymentAuditHistory.paymentAuditHistoryWith()
            .auditType(auditEventsType.getName())
            .auditPayload(target.getReference() + " | " + target.getAmount() + " | " + target.getPaymentStatus() + " | " + target.getDateCreated())
            .auditDescription(auditEventsType.getDescription())
            .build());
    }
}
