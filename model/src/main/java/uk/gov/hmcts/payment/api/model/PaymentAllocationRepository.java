package uk.gov.hmcts.payment.api.model;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface PaymentAllocationRepository extends CrudRepository<PaymentAllocation, Integer>, JpaSpecificationExecutor<PaymentAllocation> {

    <S extends PaymentAllocation> S save(S entity);
}
