package uk.gov.hmcts.payment.api.model;

import org.springframework.data.repository.CrudRepository;

public interface FeeRepository  extends CrudRepository<Fee, Integer> {

    <S extends Fee> S save(S entity);

}
