package uk.gov.hmcts.payment.api.model;

import org.springframework.data.repository.CrudRepository;
import uk.gov.hmcts.payment.api.dto.Reference;

import java.util.List;
import java.util.Optional;

public interface Payment2Repository extends CrudRepository<Payment, Integer>{

    Optional<Payment> findByReferenceAndPaymentMethod(String reference, PaymentMethod paymentMethod);

    Optional<Payment> findByReference(String reference);

    List<Reference> findReferencesByPaymentStatus(PaymentStatus paymentStatus);

}
