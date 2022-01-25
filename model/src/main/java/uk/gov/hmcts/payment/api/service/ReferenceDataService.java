package uk.gov.hmcts.payment.api.service;

import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.payment.api.dto.OrganisationalServiceDto;

import java.util.List;
import java.util.Optional;

public interface ReferenceDataService<T> {
    List<T> getSiteIDs();

    OrganisationalServiceDto getOrganisationalDetail(Optional<String> caseType, Optional<String> serviceCode, MultiValueMap<String, String> headers);

}
