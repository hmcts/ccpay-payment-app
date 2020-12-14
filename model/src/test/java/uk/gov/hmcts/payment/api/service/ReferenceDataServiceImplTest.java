package uk.gov.hmcts.payment.api.service;

import org.mockito.InjectMocks;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.payment.referencedata.model.Site;
import uk.gov.hmcts.payment.referencedata.service.SiteService;

public class ReferenceDataServiceImplTest {

    @InjectMocks
    private ReferenceDataServiceImpl referenceDataService;

    @InjectMocks
    private SiteService<Site, String> siteService;


}
