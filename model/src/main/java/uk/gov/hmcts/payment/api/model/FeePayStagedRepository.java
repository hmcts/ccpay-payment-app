package uk.gov.hmcts.payment.api.model;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface FeePayStagedRepository extends CrudRepository<FeePayStaged, Integer> {

    <S extends FeePayStaged> S save(S entity);

    Optional<FeePayStaged> findByCcdCaseNo(String ccdCaseNo);
}
