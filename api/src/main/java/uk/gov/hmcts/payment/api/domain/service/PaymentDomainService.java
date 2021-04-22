package uk.gov.hmcts.payment.api.domain.service;

import uk.gov.hmcts.payment.api.model.FeePayApportion;
import uk.gov.hmcts.payment.api.model.Payment;

public interface PaymentDomainService {
    Payment getPaymentByApportionment(FeePayApportion feePayApportion);
}
