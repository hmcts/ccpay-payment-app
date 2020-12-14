package uk.gov.hmcts.payment.api.service;

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
import uk.gov.hmcts.payment.api.dto.OrgServiceDto;
import uk.gov.hmcts.payment.api.dto.OrgServiceResponse;
import uk.gov.hmcts.payment.referencedata.dto.SiteDTO;
import uk.gov.hmcts.payment.referencedata.model.Site;
import uk.gov.hmcts.payment.referencedata.service.SiteService;

import java.util.List;

@Service
public class ReferenceDataServiceImpl implements ReferenceDataService<SiteDTO> {

    @Autowired
    private SiteService<Site, String> siteService;

    @Autowired()
    @Qualifier("restTemplatePaymentGroup")
    private RestTemplate restTemplatePaymentGroup;

    @Value("${location.ref.url}")
    private String locationReferenceUrl;

    @Override
    public List<SiteDTO> getSiteIDs() {
        return SiteDTO.fromSiteList(siteService.getAllSites());
    }

    @Override
    public String getOrgId(String caseType,MultiValueMap<String, String> headersMap) {
        ResponseEntity<OrgServiceResponse> orgServiceResponse = getResponseFromLocationReference(caseType,headersMap);
        String orgId = orgServiceResponse.getBody().getOrgServiceList().get(0).getServiceCode();
        return orgId;
    }


    private ResponseEntity<OrgServiceResponse> getResponseFromLocationReference(String caseType, MultiValueMap<String, String> headersMap){
        return restTemplatePaymentGroup.exchange(locationReferenceUrl + "/refdata/location/orgServices/{ccdCaseType}", HttpMethod.GET, getRequestHeaders(headersMap), OrgServiceResponse.class);
    }

    private HttpEntity<String> getRequestHeaders(MultiValueMap<String, String> headersMap){
        MultiValueMap<String, String> locationRefHeader = new LinkedMultiValueMap<String, String>();
        //User token
        locationRefHeader.put("Authorization", headersMap.get("authorization"));
        //Service token
        locationRefHeader.put("ServiceAuthorization", headersMap.get("serviceAuthorization"));
        HttpHeaders headers = new HttpHeaders(locationRefHeader);
        final HttpEntity<String> entity = new HttpEntity<>(headers);
        return entity;

    }
}
