package uk.gov.hmcts.payment.api.jpaaudit.listner;

import uk.gov.hmcts.payment.api.jpaaudit.model.AuditEventsType;
import uk.gov.hmcts.payment.api.jpaaudit.model.PaymentAuditHistory;
import uk.gov.hmcts.payment.api.model.FeePayApportion;

import javax.persistence.EntityManager;
import javax.persistence.PrePersist;
import javax.transaction.Transactional;

import static javax.transaction.Transactional.TxType.MANDATORY;
import static uk.gov.hmcts.payment.api.jpaaudit.listner.Action.INSERTED;
import static uk.gov.hmcts.payment.api.jpaaudit.model.AuditEventsType.PAYMENT_APPORTIONED;

public class FeePayApportionEntityListener {

    @PrePersist
    public void prePersist(FeePayApportion target) { perform(target, INSERTED, PAYMENT_APPORTIONED); }

    @Transactional(MANDATORY)
    private void perform(FeePayApportion target, Action action, AuditEventsType auditEventsType) {
        EntityManager entityManager = BeanUtil.getBean(EntityManager.class);
        entityManager.persist(PaymentAuditHistory.paymentAuditHistoryWith()
            .auditType(auditEventsType.getName())
            .auditPayload(target.getFeeId() + " | " + target.getPaymentId() + " | " + target.getApportionAmount() + " | " + target.getAllocatedAmount())
            .auditDescription(auditEventsType.getDescription())
            .build());
    }
}
