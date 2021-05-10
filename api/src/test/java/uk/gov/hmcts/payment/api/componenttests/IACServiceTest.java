package uk.gov.hmcts.payment.api.componenttests;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.FieldSetter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.dto.*;
import uk.gov.hmcts.payment.api.service.AccountServiceImpl;
import uk.gov.hmcts.payment.api.service.IACServiceImpl;
import uk.gov.hmcts.payment.api.util.AccountStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class IACServiceTest {

    @Mock
    private RestTemplate restTemplateIacSupplementaryInfo;

    @Mock
    private OAuth2RestOperations restTemplateMock;

    @InjectMocks
    private IACServiceImpl iacServiceImpl;

    @Value("${iac.supplementary.info.url}")
    private String iacSupplementaryInfoUrl;

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(9190);

    @Test
    public void retrieveSupplementaryInfo() throws Exception {

        FieldSetter.setField(iacServiceImpl, iacServiceImpl.getClass().getDeclaredField("iacSupplementaryInfoUrl"), iacSupplementaryInfoUrl);

        List<String> iacCcdCaseNos = new ArrayList<String>();
        iacCcdCaseNos.add("ccdCaseNo1");

        IacSupplementaryRequest iacSupplementaryRequest = IacSupplementaryRequest.createIacSupplementaryRequestWith()
            .ccdCaseNumbers(iacCcdCaseNos).build();

        List<String> serviceAuthTokenPaymentList = new ArrayList<>();
        //Generate token for payment api and replace
        serviceAuthTokenPaymentList.add("S2SToken");
        MultiValueMap<String, String> headerMultiValueMapForIacSuppInfo = new LinkedMultiValueMap<String, String>();
        headerMultiValueMapForIacSuppInfo.put("ServiceAuthorization", serviceAuthTokenPaymentList);
        HttpHeaders headers = new HttpHeaders(headerMultiValueMapForIacSuppInfo);
        final HttpEntity<IacSupplementaryRequest> entity = new HttpEntity<>(iacSupplementaryRequest, headers);
        SupplementaryDetailsResponse supplementaryDetailsResponse = populateIACSupplementaryDetails("ccdCaseNo1");
       when(restTemplateIacSupplementaryInfo.exchange(iacSupplementaryInfoUrl +  "/supplementary-details" , HttpMethod.POST, entity, SupplementaryDetailsResponse.class)).thenReturn(new ResponseEntity(supplementaryDetailsResponse, HttpStatus.OK));
        ResponseEntity<SupplementaryDetailsResponse> responseEntity = iacServiceImpl.getIacSupplementaryInfo(iacCcdCaseNos,"S2SToken");
        assertEquals(supplementaryDetailsResponse.getSupplementaryInfo(), responseEntity.getBody().getSupplementaryInfo());
        assertEquals(supplementaryDetailsResponse.getMissingSupplementaryInfo(), responseEntity.getBody().getMissingSupplementaryInfo());

    }

    public SupplementaryDetailsResponse populateIACSupplementaryDetails(String ccdCaseNo) throws Exception {

        SupplementaryDetailsDto supplementaryDetailsDto = SupplementaryDetailsDto.supplementaryDetailsDtoWith()
            .surname("Alex").build();

        SupplementaryInfoDto supplementaryInfoDto = SupplementaryInfoDto.supplementaryInfoDtoWith()
            .ccdCaseNumber(ccdCaseNo)
            .supplementaryDetails(supplementaryDetailsDto)
            .build();

        List<SupplementaryInfoDto> supplementaryInfoDtoList = new ArrayList<>();
        supplementaryInfoDtoList.add(supplementaryInfoDto);

        SupplementaryDetailsResponse supplementaryDetailsResponse = SupplementaryDetailsResponse.supplementaryDetailsResponseWith()
            .supplementaryInfo(supplementaryInfoDtoList)
            .build();

        return supplementaryDetailsResponse;

    }
}
