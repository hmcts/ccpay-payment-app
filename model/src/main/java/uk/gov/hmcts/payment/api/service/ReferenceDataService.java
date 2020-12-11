package uk.gov.hmcts.payment.api.service;

import java.util.List;

public interface ReferenceDataService<T> {
    List<T> getSiteIDs();

    String getOrgId(String caseType);
}
