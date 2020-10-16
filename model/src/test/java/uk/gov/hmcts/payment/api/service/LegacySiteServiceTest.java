package uk.gov.hmcts.payment.api.service;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.payment.api.model.LegacySite;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class LegacySiteServiceTest {

    private static final Logger LOG = LoggerFactory.getLogger(LegacySiteServiceTest.class);
    
    @InjectMocks
    @Autowired
    private LegacySiteServiceImpl legacySiteService;

    @Test
    public void GetLegacySites() throws Exception {
        List<LegacySite> legacySites = legacySiteService.getAllSites();
        assertThat(legacySites).isNotNull();
        LegacySite legacySite = legacySites.get(0);
        assertThat(legacySite.getSiteId()).isEqualTo("Y402");
    }
}
