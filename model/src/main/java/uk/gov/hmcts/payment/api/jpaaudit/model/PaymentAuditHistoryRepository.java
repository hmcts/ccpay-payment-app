package uk.gov.hmcts.payment.api.jpaaudit.model;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface PaymentAuditHistoryRepository extends CrudRepository<PaymentAuditHistory, Integer> {
    <S extends PaymentAuditHistory> S save(S entity);

    List<PaymentAuditHistory> findByCcdCaseNo(String ccdCaseNo);
}
