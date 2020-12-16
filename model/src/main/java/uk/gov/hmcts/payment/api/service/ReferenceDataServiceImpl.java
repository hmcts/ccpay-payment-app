package uk.gov.hmcts.payment.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import uk.gov.hmcts.payment.api.dto.OrgServiceDto;
import uk.gov.hmcts.payment.referencedata.dto.SiteDTO;
import uk.gov.hmcts.payment.referencedata.model.Site;
import uk.gov.hmcts.payment.referencedata.service.SiteService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ReferenceDataServiceImpl implements ReferenceDataService<SiteDTO> {

    private static final Logger LOG = LoggerFactory.getLogger(ReferenceDataServiceImpl.class);

    @Autowired
    private SiteService<Site, String> siteService;

    @Autowired
    private RestTemplate restTemplatePaymentGroup;

    @Value("${location.ref.url}")
    private String locationReferenceUrl;

    @Override
    public List<SiteDTO> getSiteIDs() {
        return SiteDTO.fromSiteList(siteService.getAllSites());
    }

    @Override
    public String getOrgId(String caseType, MultiValueMap<String, String> headers) {
        ResponseEntity<OrgServiceDto[]> orgServiceResponse = getResponseFromLocationReference(caseType,headers);
        OrgServiceDto[] orgServiceList = orgServiceResponse.getBody();
        String orgId = orgServiceList[0].getServiceCode();
        return orgId;
    }

    private ResponseEntity<OrgServiceDto[]> getResponseFromLocationReference(String ccdCaseType, MultiValueMap<String, String>headers){
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(locationReferenceUrl + "/refdata/location/orgServices")
            .queryParam("ccdCaseType", ccdCaseType);
        return restTemplatePaymentGroup.exchange(builder.toUriString(), HttpMethod.GET, getRequestHeaders(headers), OrgServiceDto[].class);
    }

    private HttpEntity<String> getRequestHeaders(MultiValueMap<String, String> headers) {
        MultiValueMap<String, String> headerMultiValueMap = new LinkedMultiValueMap<String, String>();
        headerMultiValueMap.put("Authorization", Collections.singletonList("Bearer " + headers.getFirst("authorization")));
        headerMultiValueMap.put("ServiceAuthorization",headers.get("serviceauthorization"));
        HttpHeaders httpHeaders= new HttpHeaders(headerMultiValueMap);
        return new HttpEntity<>(httpHeaders);
    }
}
