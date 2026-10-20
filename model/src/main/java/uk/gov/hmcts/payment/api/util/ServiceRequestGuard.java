package uk.gov.hmcts.payment.api.util;

import uk.gov.hmcts.payment.api.exceptions.ServiceRequestCancellationException;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

public final class ServiceRequestGuard {

    private ServiceRequestGuard() {
    }

    public static void requireNotCancelled(PaymentFeeLink serviceRequest) {
        if (serviceRequest.isCancelled()) {
            throw new ServiceRequestCancellationException();
        }
    }
}
