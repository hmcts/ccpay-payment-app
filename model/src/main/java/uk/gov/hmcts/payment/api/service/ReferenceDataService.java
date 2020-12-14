package uk.gov.hmcts.payment.api.service;

import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

public interface ReferenceDataService<T> {
    List<T> getSiteIDs();

    String getOrgId(String caseType, Map<String, String> headersMap);
}
