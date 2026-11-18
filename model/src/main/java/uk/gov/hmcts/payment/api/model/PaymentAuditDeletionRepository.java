package uk.gov.hmcts.payment.api.model;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface PaymentAuditDeletionRepository extends CrudRepository<PaymentAuditDeletion, Integer> {

    Optional<PaymentAuditDeletion> findFirstByCcdCaseNumberOrderByDeletionDateDesc(String ccdCaseNumber);
}
