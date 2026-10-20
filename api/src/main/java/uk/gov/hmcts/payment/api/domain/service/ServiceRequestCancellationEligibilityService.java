package uk.gov.hmcts.payment.api.domain.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

@Service
public class ServiceRequestCancellationEligibilityService {

    /**
     * Result of checking whether a service request can be cancelled.
     *
     * @param cancellable true if the service request can be cancelled
     * @param reason      why the service request cannot be cancelled; returned to the caller as the 409 error,
     *                    so it is required when {@code cancellable} is false and must be null otherwise
     */
    public record Eligibility(boolean cancellable, String reason) {

        public Eligibility {
            if (cancellable && reason != null) {
                throw new IllegalArgumentException("a cancellable service request must not have a reason");
            }
            if (!cancellable && (reason == null || reason.isBlank())) {
                throw new IllegalArgumentException("a reason is required when a service request cannot be cancelled");
            }
        }

        public static Eligibility eligible() {
            return new Eligibility(true, null);
        }

        public static Eligibility ineligible(String reason) {
            return new Eligibility(false, reason);
        }
    }

    public Eligibility checkEligibility(PaymentFeeLink paymentFeeLink) {

        // TODO PAY-9025: implement the cancellation eligibility rules.

        return Eligibility.eligible();
    }
}
