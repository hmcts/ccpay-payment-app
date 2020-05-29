package uk.gov.hmcts.payment.api.model;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface FeePayApportionRepository extends CrudRepository<FeePayApportion, Integer> {

    Optional<FeePayApportion> findByFeeIdAndPaymentId(Integer feeId, Integer paymentId);

    Optional<FeePayApportion> findByFeeId(String id);
}
