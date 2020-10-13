package uk.gov.hmcts.payment.referencedata.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface LegacySiteRepository extends JpaRepository<LegacySite, String>, JpaSpecificationExecutor<LegacySite> {

    Optional<LegacySite> findBySiteCode(String siteCode);
}
