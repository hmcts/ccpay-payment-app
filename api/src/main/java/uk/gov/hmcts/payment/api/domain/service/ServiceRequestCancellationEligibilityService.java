package uk.gov.hmcts.payment.api.domain.service;

import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentAllocation;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    // No successful payment on the SR: Reject if any payment on the SR has status success
    // No payment in progress: Reject if any payment status is one of pending, created, started
    // Failed payments are OK: Statuses failed, error, decline, cancelled do not block

    // Map payment status names to whether they are eligible for cancellation
    private static final Map<String, Eligibility> eliMap = Map.of(
        "created", Eligibility.ineligible("Created ineligible for Cancellation"),
        "success", Eligibility.ineligible("Success ineligible for Cancellation"),
        "pending", Eligibility.ineligible("Pending ineligible for Cancellation"),
        "decline", Eligibility.eligible(),
        "cancelled", Eligibility.eligible(),
        "error", Eligibility.eligible(),
        "failed", Eligibility.eligible()
    );

    private static final Eligibility SR_ALREADY_CANCELLED = Eligibility.eligible();
    private static final Eligibility UNKNOWN_PAYMENT_STATUS = Eligibility.ineligible("Unknown payment status ineligible for Cancellation");
    private static final Eligibility REMISSIONS_EXISTING_ON_SR = Eligibility.ineligible("remissions existing on SR");
    private static final Eligibility PAYMENT_ALLOCATION_EXISTING_ON_SR = Eligibility.ineligible("payment allocation existing on SR");

    // implement the cancellation eligibility rules.
    public Eligibility checkEligibility(PaymentFeeLink paymentFeeLink) {

        if (paymentFeeLink.isCancelled()) {
            return SR_ALREADY_CANCELLED;
        }

        Optional<Eligibility> inEligiblePayments = paymentFeeLink.getPayments()
            .stream()
            .map(Payment::getPaymentStatus)
            .map(s -> eliMap.getOrDefault(s.getName(), UNKNOWN_PAYMENT_STATUS))
            .filter( e -> !e.cancellable() )
            .findFirst();

        if (inEligiblePayments.isPresent()) {
            // TODO check the spec for return value
            return Eligibility.ineligible("payment status ineligible for cancellation: %s"
                .formatted(inEligiblePayments.get()));
        }

        // Remissions: Reject if the SR has any remission row where payment_link_id = SR id (conservative superset of
        //  "full remission out of scope")
        //  I.e. block if any remissions are being used for the SR, partial or full.
        if (!paymentFeeLink.getRemissions().isEmpty()) {
            // TODO is this check enough?
            return REMISSIONS_EXISTING_ON_SR;
        }

        // No unallocated payment on the case Reject: if any payment_allocation record exists against payments on the SR
        //   additionally query bulk-scanning app for pending/unallocated payments against the Case.
        List<PaymentAllocation> list = paymentFeeLink.getPayments()
            .stream()
            .map(Payment::getPaymentAllocation)
            .flatMap(List::stream)
            .toList();

        if (!list.isEmpty()) {
            return PAYMENT_ALLOCATION_EXISTING_ON_SR;
        }

        // Must be eligible

        return Eligibility.eligible();
    }
}
