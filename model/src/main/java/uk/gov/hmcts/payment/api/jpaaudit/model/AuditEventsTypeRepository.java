package uk.gov.hmcts.payment.api.jpaaudit.model;

import org.springframework.data.repository.CrudRepository;

public interface AuditEventsTypeRepository extends CrudRepository<AuditEventsType, Integer> {
}
