package uk.gov.hmcts.payment.api.service;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import uk.gov.hmcts.payment.api.dto.RemissionServiceRequest;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.Remission;

public interface RemissionService {

    PaymentFeeLink createRemission(RemissionServiceRequest remissionServiceRequest) throws CheckDigitException;

    PaymentFeeLink createRetrospectiveRemission(RemissionServiceRequest remissionServiceRequest, String paymentGroupReference) throws CheckDigitException;

    Remission retrieve(String hwfReference);
}
