package uk.gov.hmcts.payment.api.componenttests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.dto.OrganisationalServiceDto;
import uk.gov.hmcts.payment.api.service.ReferenceDataService;
import uk.gov.hmcts.payment.api.service.ReferenceDataServiceImpl;
import uk.gov.hmcts.payment.api.v1.model.exceptions.NoServiceFoundException;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
public class ReferenceDataServiceTest extends PaymentsDataUtil {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    @Qualifier("restTemplatePaymentGroup")
    private RestTemplate restTemplate = new RestTemplate();

    @InjectMocks
    private ReferenceDataService referenceDataServiceImp = new ReferenceDataServiceImpl();

    @Test(expected = NoServiceFoundException.class)
    public void getOrganisationalDetailSuccess() throws Exception {

        MultiValueMap<String, String> header = new LinkedMultiValueMap<String, String>();
        //User token
        header.put("Authorization", Collections.singletonList("Bearer 131313"));
        //Service token
        header.put("ServiceAuthorization", Collections.singletonList("qwertyuio.poiuytrewq.zxfghimbfdw"));
        header.put("Content-Type", Collections.singletonList("application/json"));

        OrganisationalServiceDto organisationalServiceDto = OrganisationalServiceDto.orgServiceDtoWith()
            .serviceCode("VPAA")
            .serviceDescription("New description")
            .ccdCaseTypes(Collections.singletonList("VPAA"))
            .build();
        List<OrganisationalServiceDto> organisationalServiceDtos = Collections.singletonList(organisationalServiceDto);

        when(restTemplate.exchange(Mockito.anyString(),
            Mockito.<HttpMethod>eq(HttpMethod.GET),
            Matchers.<HttpEntity<?>>any(),
            Mockito.<Class>any()).getBody()).thenReturn(organisationalServiceDtos);
        OrganisationalServiceDto res = referenceDataServiceImp.getOrganisationalDetail("VPAA", header);
        assertEquals(res.getServiceCode(),"VPAA");
    }

    protected String ResponseJson() {
        return
            "{\n" +
                "  \"service_id\": 23,\n" +
                "  \"ccd_service_name\": \"Tax Appeals\",\n" +
                "  \"jurisdiction\": \"Tax Chamber\",\n" +
                "  \"last_update\": \"2021-01-07T13:58:52.082544\",\n" +
                "  \"org_unit\": \"HMCTS\",\n" +
                "  \"service_code\": \"GBP\",\n" +
                "  \"service_description\": \"Tax Appeals\",\n" +
                "  \"business_area\": \"Civil, Family and Tribunals\",\n" +
                "  \"service_short_description\": \"Tax Appeals\",\n" +
                "  \"sub_business_area\": \"Tribunals\",\n" +
                "  \"ccd_case_types\": [\n" +
                "      \"tax_exception\n" +
                "  ]\n" +
                "}";
    }
}

