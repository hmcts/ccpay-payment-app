package uk.gov.hmcts.payment.api.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LegacySiteRepository extends JpaRepository<LegacySite, String>{

    Optional<LegacySite> findBySiteId(String siteId);
}
