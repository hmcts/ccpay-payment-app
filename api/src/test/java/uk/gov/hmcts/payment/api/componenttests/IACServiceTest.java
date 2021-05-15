package uk.gov.hmcts.payment.api.componenttests;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.dto.*;
import uk.gov.hmcts.payment.api.service.IACServiceImpl;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class IACServiceTest {

    /*@Mock
    private RestTemplate restTemplateIacSupplementaryInfo;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @InjectMocks
    private IACServiceImpl iacServiceImpl;


    @Test
    public void retrieveSupplementaryInfo() throws Exception {
        SupplementaryDetailsResponse supplementaryDetailsResponse = populateIACSupplementaryDetails("ccdCaseNo1");
        List<String> iacCcdCaseNos = new ArrayList<String>();
        iacCcdCaseNos.add("ccdCaseNo1");
        when(this.restTemplateIacSupplementaryInfo.exchange(anyString(),eq(HttpMethod.POST),any(HttpEntity.class),eq(SupplementaryDetailsResponse.class)))
            .thenReturn(new ResponseEntity(supplementaryDetailsResponse,HttpStatus.OK));

        ResponseEntity<SupplementaryDetailsResponse> responseEntity = iacServiceImpl.getIacSupplementaryInfoResponse(iacCcdCaseNos);
        assertEquals(supplementaryDetailsResponse.getSupplementaryInfo(), responseEntity.getBody().getSupplementaryInfo());
        assertEquals(supplementaryDetailsResponse.getMissingSupplementaryInfo(), responseEntity.getBody().getMissingSupplementaryInfo());
    }

    public SupplementaryDetailsResponse populateIACSupplementaryDetails(String ccdCaseNo)  {

        SupplementaryDetails supplementaryDetailsDto = SupplementaryDetails.supplementaryDetailsWith()
            .surname("Alex").build();

        SupplementaryInfo supplementaryInfoDto = SupplementaryInfo.supplementaryInfoWith()
            .ccdCaseNumber(ccdCaseNo)
            .supplementaryDetails(supplementaryDetailsDto)
            .build();

        List<SupplementaryInfo> supplementaryInfoDtoList = new ArrayList<>();
        supplementaryInfoDtoList.add(supplementaryInfoDto);

        SupplementaryDetailsResponse supplementaryDetailsResponse = SupplementaryDetailsResponse.supplementaryDetailsResponseWith()
            .supplementaryInfo(supplementaryInfoDtoList)
            .build();

        return supplementaryDetailsResponse;

    }*/

    @Test
    public void retrieveSupplementaryInfo() throws Exception {

    }
}
