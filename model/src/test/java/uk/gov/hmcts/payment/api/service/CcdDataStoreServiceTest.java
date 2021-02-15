package uk.gov.hmcts.payment.api.service;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.api.service.CcdDataStoreClientServiceImpl;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,classes=CcdDataStoreClientServiceImpl.class)
public class CcdDataStoreServiceTest {
    @MockBean
    private CoreCaseDataApi coreCaseDataApi;

    @InjectMocks
    @Autowired
    private CcdDataStoreClientServiceImpl ccdDataStoreService;

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(9190);

    @Test
    public void retrieveExistingCcdCaseReferenceReturnsCcdCaseDetails() throws Exception {
        String userAuthToken = "userToken";
        String serviceAuthToken = "serviceToken";
        String ccdCaseReference = "test-ok";
        CaseDetails expectedCaseDetails = CaseDetails.builder().jurisdiction("juristiction").build();

        when(coreCaseDataApi.getCase(userAuthToken, serviceAuthToken, ccdCaseReference)).thenReturn(expectedCaseDetails);
        assertEquals(expectedCaseDetails, ccdDataStoreService.getCase(userAuthToken, serviceAuthToken, ccdCaseReference));
    }
}
