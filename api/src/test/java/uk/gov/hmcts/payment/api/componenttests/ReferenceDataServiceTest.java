package uk.gov.hmcts.payment.api.componenttests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
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
import uk.gov.hmcts.payment.referencedata.model.Site;
import uk.gov.hmcts.payment.referencedata.service.SiteService;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
public class ReferenceDataServiceTest extends PaymentsDataUtil {

    static MultiValueMap<String, String> header = new LinkedMultiValueMap<String, String>();
    @Mock
    SiteService<Site, String> siteService;
    @InjectMocks
    ReferenceDataServiceImpl referenceDataService;
    @MockBean
    @Qualifier("restTemplatePaymentGroup")
    private RestTemplate restTemplate;
    @MockBean
    private AuthTokenGenerator authTokenGenerator;
    @Autowired
    private ReferenceDataService referenceDataServiceImp;

    @Before
    public void setup() {
        when(authTokenGenerator.generate()).thenReturn("test-token");
        //User token
        header.put("Authorization", Collections.singletonList("Bearer 131313"));
        //Service token
        header.put("ServiceAuthorization", Collections.singletonList("qwertyuio.poiuytrewq.zxfghimbfdw"));
        header.put("Content-Type", Collections.singletonList("application/json"));
    }

    @After
    public void tearDown() {
        header.clear();
    }

    @Test
    public void testGetSiteIDs() {
        Site site1 = Site.siteWith()
            .name("site1")
            .build();
        List<Site> siteList = new ArrayList<Site>();
        siteList.add(site1);
        when(siteService.getAllSites()).thenReturn(siteList);
        referenceDataService.getSiteIDs();
        verify(siteService).getAllSites();
    }

    @Test
    public void getOrganisationalDetailSuccess() throws Exception {

        OrganisationalServiceDto organisationalServiceDto = OrganisationalServiceDto.orgServiceDtoWith()
            .serviceCode("VPAA")
            .serviceDescription("DIVORCE")
            .ccdCaseTypes(Collections.singletonList("VPAA"))
            .build();
        List<OrganisationalServiceDto> organisationalServiceDtos = new ArrayList<>();
        organisationalServiceDtos.add(organisationalServiceDto);
        ResponseEntity<List<OrganisationalServiceDto>> responseEntity = new ResponseEntity<>(organisationalServiceDtos, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(new ParameterizedTypeReference<List<OrganisationalServiceDto>>() {
            }))).thenReturn(responseEntity);
        OrganisationalServiceDto res = referenceDataServiceImp.getOrganisationalDetail(Optional.ofNullable("VPAA"),Optional.empty(), header);
            eq(new ParameterizedTypeReference<List<OrganisationalServiceDto>>() {
            }))).thenReturn(responseEntity);
        OrganisationalServiceDto res = referenceDataServiceImp.getOrganisationalDetail("VPAA", header);
        assertEquals("VPAA", res.getServiceCode());
    }

    @Test(expected = NoServiceFoundException.class)
    public void getOrganisationalDetailNoServiceFound() throws Exception {

        MultiValueMap<String, String> header = new LinkedMultiValueMap<String, String>();
        //User token
        // changed to lower case to depict real time header
        header.put("authorization", Collections.singletonList("Bearer 131313"));
        //Service token
        header.put("ServiceAuthorization", Collections.singletonList("qwertyuio.poiuytrewq.zxfghimbfdw"));
        header.put("Content-Type", Collections.singletonList("application/json"));

        ResponseEntity<List<OrganisationalServiceDto>> responseEntity = new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK);
        ResponseEntity<List<OrganisationalServiceDto>> responseEntity = new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK);

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(new ParameterizedTypeReference<List<OrganisationalServiceDto>>() {
            }))).thenReturn(responseEntity);
        OrganisationalServiceDto res = referenceDataServiceImp.getOrganisationalDetail("VPAA", header);
            eq(new ParameterizedTypeReference<List<OrganisationalServiceDto>>() {
            }))).thenReturn(responseEntity);
        OrganisationalServiceDto res = referenceDataServiceImp.getOrganisationalDetail(Optional.ofNullable("VPAA"),Optional.empty(), header);
    }

    @Test(expected = NoServiceFoundException.class)
    public void getOrganisationalDetailNoServiceFoundWithNull() throws Exception {

        ResponseEntity<List<OrganisationalServiceDto>> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);
        MultiValueMap<String, String> header = new LinkedMultiValueMap<String, String>();
        //User token
        header.put("Authorization", Collections.singletonList("Bearer 131313"));
        //Service token
        header.put("ServiceAuthorization", Collections.singletonList("qwertyuio.poiuytrewq.zxfghimbfdw"));
        header.put("Content-Type", Collections.singletonList("application/json"));

        ResponseEntity<List<OrganisationalServiceDto>> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
            eq(new ParameterizedTypeReference<List<OrganisationalServiceDto>>() {
            }))).thenReturn(responseEntity);
        OrganisationalServiceDto res = referenceDataServiceImp.getOrganisationalDetail("VPAA", header);
            eq(new ParameterizedTypeReference<List<OrganisationalServiceDto>>() {
            }))).thenReturn(responseEntity);
        OrganisationalServiceDto res = referenceDataServiceImp.getOrganisationalDetail(Optional.ofNullable("VPAA"), Optional.empty(), header);
    }

}

