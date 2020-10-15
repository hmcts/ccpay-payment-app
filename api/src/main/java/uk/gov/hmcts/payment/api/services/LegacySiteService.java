package uk.gov.hmcts.payment.api.services;

import uk.gov.hmcts.payment.api.model.LegacySite;

import java.util.List;

public interface LegacySiteService<T, I>  {

    List<LegacySite> getAllSites();
}
