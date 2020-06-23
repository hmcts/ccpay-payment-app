package uk.gov.hmcts.payment.api.jpaaudit.listner;

import uk.gov.hmcts.payment.api.jpaaudit.model.AuditEventsType;
import uk.gov.hmcts.payment.api.jpaaudit.model.PaymentAuditHistory;
import uk.gov.hmcts.payment.api.model.PaymentAllocation;

import javax.persistence.EntityManager;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.transaction.Transactional;

import static javax.transaction.Transactional.TxType.MANDATORY;
import static uk.gov.hmcts.payment.api.jpaaudit.listner.Action.INSERTED;
import static uk.gov.hmcts.payment.api.jpaaudit.listner.Action.UPDATED;
import static uk.gov.hmcts.payment.api.jpaaudit.model.AuditEventsType.*;

public class PaymentAllocationEntityListener {

    @PostPersist
    public void postPersist(PaymentAllocation target) {
        perform(target, INSERTED, PAYMENT_ALLOCATION_CREATED);
    }

    @PostUpdate
    public void postUpdate(PaymentAllocation target) {
        perform(target, UPDATED, PAYMENT_ALLOCATION_UPDATED);
    }

    @Transactional(MANDATORY)
    private void perform(PaymentAllocation target, Action action, AuditEventsType auditEventsType) {
        EntityManager entityManager = BeanUtil.getBean(EntityManager.class);
        entityManager.persist(PaymentAuditHistory.paymentAuditHistoryWith()
            .ccdCaseNo(target.getPayment().getCcdCaseNumber())
            .auditType(auditEventsType.getName())
            //.auditPayload(target.getReference() + " | " + target.getAmount() + " | " + target.getPaymentStatus() + " | " + target.getDateCreated())
            .auditPayload(target.toString())
            .auditDescription(auditEventsType.getDescription())
            .build());
    }
}
