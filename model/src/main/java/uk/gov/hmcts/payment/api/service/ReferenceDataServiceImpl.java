package uk.gov.hmcts.payment.api.service;

import com.launchdarkly.shaded.kotlin.collections.EmptyList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.payment.api.dto.OrganisationalServiceDto;
import uk.gov.hmcts.payment.api.v1.model.exceptions.NoServiceFoundException;
import uk.gov.hmcts.payment.referencedata.dto.SiteDTO;
import uk.gov.hmcts.payment.referencedata.model.Site;
import uk.gov.hmcts.payment.referencedata.service.SiteService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ReferenceDataServiceImpl implements ReferenceDataService<SiteDTO> {

    @Autowired
    private SiteService<Site, String> siteService;

    @Autowired
    private RestTemplate restTemplatePaymentGroup;

    @Value("${rd.location.url}")
    private String rdBaseUrl;

    @Override
    public List<SiteDTO> getSiteIDs() {
        return SiteDTO.fromSiteList(siteService.getAllSites());
    }

    @Override
    public OrganisationalServiceDto getOrganisationalDetail(String caseType, HttpEntity<String> headers) throws NoServiceFoundException {
            ResponseEntity<OrganisationalServiceDto[]> orgServiceResponse = getResponseFromLocationReference(caseType, headers);
            OrganisationalServiceDto[] orgServiceList = orgServiceResponse.getBody();
            if(orgServiceList.length > 0){
                return orgServiceList[0];
            }else{
                throw new NoServiceFoundException( "No Service found for given CaseType");
            }
    }


    public ResponseEntity<OrganisationalServiceDto[]> getResponseFromLocationReference(String ccdCaseType, HttpEntity<String> headers) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(rdBaseUrl + "/refdata/location/orgServices")
            .queryParam("ccdCaseType", ccdCaseType);
        return restTemplatePaymentGroup.exchange(builder.toUriString(), HttpMethod.GET, headers, OrganisationalServiceDto[].class);
    }
}
