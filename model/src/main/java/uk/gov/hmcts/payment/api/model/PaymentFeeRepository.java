package uk.gov.hmcts.payment.api.model;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface PaymentFeeRepository extends CrudRepository<PaymentFee, Integer>, JpaSpecificationExecutor<PaymentFee> {

    <S extends PaymentFee> S save(S entity);

    Optional<PaymentFee> findByReference(String reference);

    Optional<PaymentFee> findByPaymentLinkId(Integer id);

}
