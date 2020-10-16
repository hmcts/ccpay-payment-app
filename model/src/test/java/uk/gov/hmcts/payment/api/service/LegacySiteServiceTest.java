package uk.gov.hmcts.payment.api.service;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.payment.api.model.LegacySite;
import uk.gov.hmcts.payment.api.model.LegacySiteRepository;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class LegacySiteServiceTest {

    @Mock
    private LegacySiteRepository legacySiteRepository;

    @InjectMocks
    private LegacySiteServiceImpl legacySiteService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void GetLegacySites() throws Exception {
        List<LegacySite> legacySites = new ArrayList<LegacySite>();
        legacySites.add(new LegacySite("Y402", "gad seed"));
        Mockito.when(legacySiteService.getAllSites()).thenReturn(legacySites);
        assertThat(legacySiteService.getAllSites()).isNotNull();
        LegacySite legacySite = legacySiteService.getAllSites().get(0);
        assertThat(legacySite.getSiteId()).isEqualTo("Y402");
    }
}
