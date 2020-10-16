package uk.gov.hmcts.payment.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.model.LegacySite;
import uk.gov.hmcts.payment.api.model.LegacySiteRepository;

import java.util.List;

@Service
public class LegacySiteServiceImpl implements LegacySiteService<LegacySite, String> {

    @Autowired
    private LegacySiteRepository legacySiteRepository;

    @Override
    public List<LegacySite> getAllSites() {
        return legacySiteRepository.findAll();
    }
}

