package uk.gov.hmcts.payment.api.jpaaudit.listner;

import uk.gov.hmcts.payment.api.jpaaudit.model.AuditEventsType;
import uk.gov.hmcts.payment.api.jpaaudit.model.PaymentAuditHistory;
import uk.gov.hmcts.payment.api.model.CaseDetails;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

import javax.persistence.EntityManager;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.transaction.Transactional;

import static javax.transaction.Transactional.TxType.MANDATORY;
import static uk.gov.hmcts.payment.api.jpaaudit.listner.Action.INSERTED;
import static uk.gov.hmcts.payment.api.jpaaudit.listner.Action.UPDATED;
import static uk.gov.hmcts.payment.api.jpaaudit.model.AuditEventsType.ORDER_CREATED;

public class CaseDetailsEntityListener {

    @PostPersist
    public void postPersist(CaseDetails target) {
        perform(target, INSERTED, ORDER_CREATED);
    }

    @Transactional(MANDATORY)
    private void perform(CaseDetails target, Action action, AuditEventsType auditEventsType) {
        EntityManager entityManager = BeanUtil.getBean(EntityManager.class);
        String ccdCaseNumber = target.getCcdCaseNumber();
        entityManager.persist(PaymentAuditHistory.paymentAuditHistoryWith()
            .ccdCaseNo(ccdCaseNumber)
            .auditType(auditEventsType)
            .auditPayload(target.toString())
            .auditDescription(auditEventsType.getDescription())
            .build());
    }
}
