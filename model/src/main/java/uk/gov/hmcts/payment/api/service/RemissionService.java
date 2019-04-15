package uk.gov.hmcts.payment.api.service;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import uk.gov.hmcts.payment.api.dto.RemissionServiceRequest;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.Remission;

public interface RemissionService {

    PaymentFeeLink create(RemissionServiceRequest remissionServiceRequest) throws CheckDigitException;

    Remission retrieve(String hwfReference);
}
