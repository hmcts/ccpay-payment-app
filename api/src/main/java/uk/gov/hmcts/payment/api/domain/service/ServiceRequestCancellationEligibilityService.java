package uk.gov.hmcts.payment.api.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentAllocation;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
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
        private static final Eligibility ELIGIBLE = new Eligibility(true, null);

        public Eligibility {
            if (cancellable && reason != null) {
                throw new IllegalArgumentException("a cancellable service request must not have a reason");
            }
            if (!cancellable && (reason == null || reason.isBlank())) {
                throw new IllegalArgumentException("a reason is required when a service request cannot be cancelled");
            }
        }

        public static Eligibility eligible() {
            return ELIGIBLE;
        }

        public static Eligibility ineligible(String reason) {
            return new Eligibility(false, reason);
        }
    }

    private static final Eligibility UNKNOWN_PAYMENT_STATUS = Eligibility.ineligible("unknown payment status");
    private static final Eligibility REMISSIONS_EXISTING_ON_SR = Eligibility.ineligible("remissions existing on SR");
    private static final Eligibility PAYMENT_ALLOCATION_EXISTING_ON_SR = Eligibility.ineligible("payment allocation existing on SR");

    // Map payment status names to whether they are eligible for cancellation
    private static final Map<String, Eligibility> eliMap = Map.of(
        "created",    Eligibility.ineligible("payment with status created"),
        "started",    Eligibility.ineligible("payment with status started"),
        "success",    Eligibility.ineligible("payment with status success"),
        "pending",    Eligibility.ineligible("payment with status pending"),
        "submitted",  Eligibility.ineligible("payment with status submitted"),
        "decline",    Eligibility.eligible(),
        "cancelled",  Eligibility.eligible(),
        "error",      Eligibility.eligible(),
        "failed",     Eligibility.eligible()
    );

    /**
     * Check if the given Service Request is eligible for cancellation
     * @param paymentFeeLink Reference to the Service Request
     * @return eligibility status including reason if not eligible
     */
    public Eligibility checkEligibility(PaymentFeeLink paymentFeeLink) {

        if (paymentFeeLink.isCancelled()) {
            return Eligibility.eligible();
        }

        // Check payment status against the map
        Optional<Eligibility> ineligiblePayment = paymentFeeLink.getPayments()
            .stream()
            .map(Payment::getPaymentStatus)
            .map(s -> eliMap.getOrDefault(s.getName(), UNKNOWN_PAYMENT_STATUS))
            .filter(e -> !e.cancellable())
            .findFirst();

        if (ineligiblePayment.isPresent()) {
            log.info("{} ineligible for cancellation as {}", render(paymentFeeLink), ineligiblePayment.get().reason());
            return ineligiblePayment.get();
        }

        if (!paymentFeeLink.getRemissions().isEmpty()) {
            log.info("{} ineligible for cancellation as has remissions present", render(paymentFeeLink));
            return REMISSIONS_EXISTING_ON_SR;
        }

        // Check no unallocated payment on the case
        // if any payment_allocation record exists against payments on the SR
        Optional<PaymentAllocation> paymentAllocation = paymentFeeLink.getPayments()
            .stream()
            .map(Payment::getPaymentAllocation)
            .flatMap(List::stream)
            .findFirst();

        if (paymentAllocation.isPresent()) {
            log.info("{} ineligible for cancellation as have payment allocation {}", render(paymentFeeLink), paymentAllocation.get());
            return PAYMENT_ALLOCATION_EXISTING_ON_SR;
        }

        return checkEligibilityBulkScanning(paymentFeeLink);
    }

    // TODO make call to additionally query bulk-scanning app for pending/unallocated payments against the Case.
    // Worry about row lock time
    Eligibility checkEligibilityBulkScanning(PaymentFeeLink paymentFeeLink) {

        return Eligibility.eligible();
    }


    String render(PaymentFeeLink sr) {
        return "Service Request (id=%s, payment_reference=%s, ccd_case_number=%s)"
            .formatted(sr.getId(), sr.getPaymentReference(), sr.getCcdCaseNumber());
    }

}
