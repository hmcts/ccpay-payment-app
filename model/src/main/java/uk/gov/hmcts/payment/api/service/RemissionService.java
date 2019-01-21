package uk.gov.hmcts.payment.api.service;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import uk.gov.hmcts.payment.api.model.Remission;

public interface RemissionService {
    void create(Remission remission) throws CheckDigitException;

    Remission retrieve(String hwfReference);
}
