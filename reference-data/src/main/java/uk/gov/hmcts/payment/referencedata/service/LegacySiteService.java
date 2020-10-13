package uk.gov.hmcts.payment.referencedata.service;

import uk.gov.hmcts.payment.referencedata.model.LegacySite;

import java.util.List;

public interface LegacySiteService<T, I>  {

    List<LegacySite> getAllSites();
}
