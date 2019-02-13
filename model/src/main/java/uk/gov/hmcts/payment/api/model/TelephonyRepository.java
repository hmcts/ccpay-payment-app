package uk.gov.hmcts.payment.api.model;

import org.springframework.data.repository.CrudRepository;

public interface TelephonyRepository extends CrudRepository<TelephonyCallback, String> {
}
