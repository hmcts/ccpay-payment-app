package uk.gov.hmcts.payment.api.componenttests;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.FieldSetter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.api.dto.CcdCaseDetailsDto;
import uk.gov.hmcts.payment.api.service.CcdDataStoreServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest", "ccdMock"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class CcdDataStoreServiceTest {
    @Mock
    private OAuth2RestOperations restTemplateMock;

    @InjectMocks
    private CcdDataStoreServiceImpl ccdDataStoreService;

    @Value("${ccd-data-store.api.url}")
    private String baseUrl;

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(9190);

    @Test
    public void retrieveExistingCcdCaseReferenceReturnsCcdCaseDetailsDto() throws Exception {
        String ccdCaseReference = "test-ok";

        FieldSetter.setField(ccdDataStoreService, ccdDataStoreService.getClass().getDeclaredField("baseUrl"), baseUrl);
        CcdCaseDetailsDto expectedDto = CcdCaseDetailsDto.ccdCaseDetailsDtoWith().build();
        when(restTemplateMock.getForObject(baseUrl + "/cases/" + ccdCaseReference, CcdCaseDetailsDto.class)).thenReturn(expectedDto);
        assertEquals(expectedDto, ccdDataStoreService.retrieve(ccdCaseReference));
    }
}
