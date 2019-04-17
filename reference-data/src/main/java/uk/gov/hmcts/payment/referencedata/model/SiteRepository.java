package uk.gov.hmcts.payment.referencedata.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SiteRepository extends JpaRepository<Site, Integer>, JpaSpecificationExecutor<Site> {
    <S extends Site> S save(S entity);

    Optional<Site> findById(Integer id);

    Optional<Site> findBySiteId(String siteId);
}
