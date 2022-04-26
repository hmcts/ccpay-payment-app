package uk.gov.hmcts.payment.api.model;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface RemissionRepository extends CrudRepository<Remission, Integer>, JpaSpecificationExecutor<Remission> {
    Optional<Remission> findByHwfReference(String hwfReference);

    Optional<Remission> findByRemissionReference(String remissionReference);

    Optional<Remission> findById(Integer id);

    <S extends Remission> S save(S entity);

    Optional<Remission> findByFeeId(Integer feeid);

    void delete(Remission entity);
}
