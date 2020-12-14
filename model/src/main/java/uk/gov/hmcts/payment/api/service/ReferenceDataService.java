package uk.gov.hmcts.payment.api.service;

import org.springframework.util.MultiValueMap;

import java.util.List;

public interface ReferenceDataService<T> {
    List<T> getSiteIDs();

    String getOrgId(String caseType, MultiValueMap<String, String> headersMap);
}
