package uk.gov.hmcts.payment.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.payment.api.dto.OrganisationalServiceDto;
import uk.gov.hmcts.payment.api.v1.model.exceptions.NoServiceFoundException;
import uk.gov.hmcts.payment.referencedata.dto.SiteDTO;
import uk.gov.hmcts.payment.referencedata.model.Site;
import uk.gov.hmcts.payment.referencedata.service.SiteService;

import java.util.Arrays;
import java.util.List;

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
            if(null != orgServiceResponse  && orgServiceResponse.hasBody()){
                OrganisationalServiceDto[] organisationalServiceDtos = orgServiceResponse.getBody();
                if(organisationalServiceDtos != null && null != organisationalServiceDtos && Arrays.stream(organisationalServiceDtos).count() > 0) {
                    return organisationalServiceDtos[0];
                }else{
                    throw new NoServiceFoundException( "No Service found for given CaseType");
                }
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
