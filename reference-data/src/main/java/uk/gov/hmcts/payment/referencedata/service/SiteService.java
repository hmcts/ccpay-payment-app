package uk.gov.hmcts.payment.referencedata.service;

import uk.gov.hmcts.payment.referencedata.model.Site;

import java.util.List;

public interface SiteService<T, I> {
    T retrieve(I siteId);

    List<Site> findAll();
}
