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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.payment.api.dto.OrgServiceDto;
import uk.gov.hmcts.payment.api.dto.OrgServiceResponse;
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
    public String getOrgId(String caseType, Map<String, String> headersMap) {
        ResponseEntity<OrgServiceResponse> orgServiceResponse = getResponseFromLocationReference(caseType,headersMap);
        LOG.info("orgServiceResponse"+orgServiceResponse.getBody().toString());
        String orgId = orgServiceResponse.getBody().getOrgServiceList().get(0).getServiceCode();
        return orgId;
    }


    private ResponseEntity<OrgServiceResponse> getResponseFromLocationReference(String ccdCaseType, Map<String, String> headersMap){
        getRequestHeaders(headersMap).getHeaders().forEach((k, v) -> LOG.info(k + ": " + v));
        LOG.info(ccdCaseType);
        LOG.info(restTemplatePaymentGroup.toString());
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(locationReferenceUrl + "/refdata/location/orgServices")
            .queryParam("ccdCaseType", ccdCaseType);
        return restTemplatePaymentGroup.exchange(builder.toUriString(), HttpMethod.GET, getRequestHeaders(headersMap), OrgServiceResponse.class);
    }

    private HttpEntity<String> getRequestHeaders(Map<String, String> headersMap){
        HttpHeaders headers = new HttpHeaders();
        headersMap.forEach((key,value)->headers.set(key,value));
        final HttpEntity<String> entity = new HttpEntity<>(headers);
        return entity;

    }
}
