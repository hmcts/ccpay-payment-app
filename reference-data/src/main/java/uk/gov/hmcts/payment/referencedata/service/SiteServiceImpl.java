package uk.gov.hmcts.payment.referencedata.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.referencedata.exception.ReferenceDataNotFoundException;
import uk.gov.hmcts.payment.referencedata.model.Site;
import uk.gov.hmcts.payment.referencedata.model.SiteRepository;

import java.util.List;

@Service
public class SiteServiceImpl implements SiteService<Site, String> {

    @Autowired
    private SiteRepository siteRepository;

    @Override
    public Site retrieve(String siteId) {
        return siteRepository.findBySiteId(siteId).orElseThrow(ReferenceDataNotFoundException::new);
    }

    @Override
    @Cacheable(value = "sites", key = "#root.method.name", unless = "#result == null || #result.isEmpty()")
    public List<Site> getAllSites() {
        return siteRepository.findAll();
    }
}
