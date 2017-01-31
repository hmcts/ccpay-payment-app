package uk.gov.justice.payment.api.repository;


import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;
import uk.gov.justice.payment.api.model.PaymentHistoryEntry;


public interface PaymentHistoryRepository extends CrudRepository<PaymentHistoryEntry, Integer>, QueryDslPredicateExecutor<PaymentHistoryEntry> {
    <S extends PaymentHistoryEntry> S save(S entity);
}
