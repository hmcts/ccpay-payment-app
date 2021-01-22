package uk.gov.hmcts.payment.api.jpaaudit.model;

import org.springframework.data.repository.CrudRepository;

public interface PaymentAuditHistoryRepository extends CrudRepository<PaymentAuditHistory, Integer> {
    <S extends PaymentAuditHistory> S save(S entity);
}
