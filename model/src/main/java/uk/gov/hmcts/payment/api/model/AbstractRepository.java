package uk.gov.hmcts.payment.api.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentRefDataNotFoundException;

import java.io.Serializable;
import java.util.Optional;

@NoRepositoryBean
public interface AbstractRepository<T, ID extends Serializable> extends JpaRepository<T, ID> {
    String getEntityName();

    Optional<T> findByName(String name);

    default T findByNameOrThrow(String name) {
        return findByName(name).orElseThrow(() -> new PaymentRefDataNotFoundException(getEntityName() + " with " + name + " is not found"));
    }

}
