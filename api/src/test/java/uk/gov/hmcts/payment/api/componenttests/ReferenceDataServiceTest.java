package uk.gov.hmcts.payment.api.componenttests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.componenttests.util.PaymentsDataUtil;
import uk.gov.hmcts.payment.api.dto.OrganisationalServiceDto;
import uk.gov.hmcts.payment.api.service.ReferenceDataService;
import uk.gov.hmcts.payment.api.service.ReferenceDataServiceImpl;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;
import uk.gov.hmcts.payment.api.v1.model.exceptions.NoServiceFoundException;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.codehaus.groovy.runtime.DefaultGroovyMethods.any;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

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
        header.put("ServiceAuthorization", Collections.singletonList("defefe"));
        header.put("Content-Type", Collections.singletonList("application/json"));

        //Http headers
        HttpHeaders httpHeaders = new HttpHeaders(header);
        HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
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
        OrganisationalServiceDto res = referenceDataServiceImp.getOrganisationalDetail("VPAA", entity);
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

