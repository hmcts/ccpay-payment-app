package uk.gov.hmcts.payment.api.service;

import uk.gov.hmcts.payment.api.model.LegacySite;

import java.util.List;

public interface LegacySiteService<T, I>  {

    List<LegacySite> getAllSites();
}
