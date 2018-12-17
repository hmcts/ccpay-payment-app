package uk.gov.hmcts.payment.api.service;

import uk.gov.hmcts.payment.api.model.Remission;

public interface RemissionService {
    void create(Remission remission);

    Remission retrieve(String hwfReference);
}
