package uk.gov.hmcts.payment.api.util;

import uk.gov.hmcts.payment.api.exceptions.ServiceRequestCancellationException;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

public final class ServiceRequests {

    private ServiceRequests() {}

    public static void requireNotCancelled(PaymentFeeLink serviceRequest) {
        if (serviceRequest.isCancelled()) {
            throw new ServiceRequestCancellationException();
        }
    }
}
