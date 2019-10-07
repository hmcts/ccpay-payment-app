package uk.gov.hmcts.payment.api.service;

import uk.gov.hmcts.payment.api.model.PaymentAllocation;

public interface PaymentAllocationService {

    PaymentAllocation createAllocation(PaymentAllocation paymentAllocation);

}
