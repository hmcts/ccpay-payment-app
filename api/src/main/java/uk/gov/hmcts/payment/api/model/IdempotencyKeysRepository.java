package uk.gov.hmcts.payment.api.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface IdempotencyKeysRepository extends JpaRepository<IdempotencyKeys, IdempotencyKeysPK>, JpaSpecificationExecutor<IdempotencyKeys> {

    <S extends IdempotencyKeys> S saveAndFlush(S entity);

    Optional<IdempotencyKeys> findByIdempotencyKey(String id);

    List<IdempotencyKeys> findByRequestHashcode(Integer requestHashcode);
}
