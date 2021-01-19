package uk.gov.hmcts.payment.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.payment.api.dto.OrganisationalServiceDto;
import uk.gov.hmcts.payment.api.v1.model.exceptions.GatewayTimeoutException;
import uk.gov.hmcts.payment.api.v1.model.exceptions.NoServiceFoundException;
import uk.gov.hmcts.payment.referencedata.dto.SiteDTO;
import uk.gov.hmcts.payment.referencedata.model.Site;
import uk.gov.hmcts.payment.referencedata.service.SiteService;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ReferenceDataServiceImpl implements ReferenceDataService<SiteDTO> {

    @Autowired
    private SiteService<Site, String> siteService;

    private static final Logger LOG = LoggerFactory.getLogger(ReferenceDataService.class);

    @Autowired()
    @Qualifier("restTemplatePaymentGroup")
    private RestTemplate restTemplate;

    @Value("${rd.location.url}")
    private String rdBaseUrl;

    @Autowired
    private AuthTokenGenerator authTokenGenerator;

    @Override
    public List<SiteDTO> getSiteIDs() {
        return SiteDTO.fromSiteList(siteService.getAllSites());
    }

    @Override
    public OrganisationalServiceDto getOrganisationalDetail(String caseType, MultiValueMap<String, String> headers){

        List<String> serviceAuthTokenPaymentList = new ArrayList<>();

        MultiValueMap<String, String> headerMultiValueMapForOrganisationalDetail = new LinkedMultiValueMap<String, String>();
        List<OrganisationalServiceDto> orgServiceResponse = Collections.emptyList();
        try {
            serviceAuthTokenPaymentList.add(authTokenGenerator.generate());
            headerMultiValueMapForOrganisationalDetail.put("Content-Type", headers.get("content-type"));
            //User token
            headerMultiValueMapForOrganisationalDetail.put("Authorization", Collections.singletonList("Bearer " + headers.get("authorization")));
            //Service token
            headerMultiValueMapForOrganisationalDetail.put("ServiceAuthorization", serviceAuthTokenPaymentList);
            //Http headers
            HttpHeaders httpHeaders = new HttpHeaders(headerMultiValueMapForOrganisationalDetail);
            final HttpEntity<String> entity = new HttpEntity<>(headers);

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(rdBaseUrl + "/refdata/location/orgServices")
                .queryParam("ccdCaseType", caseType);
            ResponseEntity<List<OrganisationalServiceDto>> responseEntity = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, entity, new ParameterizedTypeReference<List<OrganisationalServiceDto>>() {
            });
            orgServiceResponse = responseEntity.getBody();
            if (orgServiceResponse == null || orgServiceResponse.isEmpty()) {
                throw new NoServiceFoundException("No Service found for given CaseType");
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            LOG.error("ORG ID Ref error status Code {} ", e.getRawStatusCode());
            if (e.getRawStatusCode() == 404) {
                throw new NoServiceFoundException("No Service found for given CaseType");
            }
            if (e.getRawStatusCode() == 504) {
                throw new GatewayTimeoutException("Unable to retrieve service information. Please try again later");
            }
        }
        return orgServiceResponse.get(0);
    }
}
