package uk.gov.hmcts.payment.referencedata.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.referencedata.model.LegacySite;
import uk.gov.hmcts.payment.referencedata.model.LegacySiteRepository;

import java.util.List;

@Service
public class LegacySiteServiceImpl implements LegacySiteService<LegacySite, String> {

    @Autowired
    private LegacySiteRepository legacySiteRepository;

    @Override
    @Cacheable(value = "legacysites", key = "#root.method.name", unless = "#result == null || #result.isEmpty()")
    public List<LegacySite> getAllSites() {
        return legacySiteRepository.findAll();
    }
}

