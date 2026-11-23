package uk.gov.hmcts.payment.api.service;

import java.util.List;

public interface PaymentDeletionAuditService {

    void recordDeletionAudits(List<PaymentDeletionAuditDetails> audits);
}
