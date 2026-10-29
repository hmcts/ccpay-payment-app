package uk.gov.hmcts.payment.api.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.payment.api.dto.PaymentStatus;
import uk.gov.hmcts.payment.api.dto.SearchResponse;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentAllocation;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.service.BulkScanningClient;

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
    private static final Eligibility UNALLOCATED_BULK_SCAN_PAYMENT_ON_CASE = Eligibility
        .ineligible("unallocated bulk scan payment existing on case");
    private static final Eligibility BULK_SCANNING_NOT_CHECKED_WITHOUT_USER_TOKEN = Eligibility
        .ineligible("unable to check bulk scanning for unallocated payments without a user token");

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

    private final BulkScanningClient bulkScanningClient;
    private final boolean bulkScanningCheckEnabled;

    /**
     * @param bulkScanningClient       client used to look for unallocated bulk-scanned payments on the case
     * @param bulkScanningCheckEnabled whether to check bulk scanning at all; on unless the property is set to false
     */
    public ServiceRequestCancellationEligibilityService(
        BulkScanningClient bulkScanningClient,
        @Value("${feature.cancellation.bulk-scanning-check:true}") boolean bulkScanningCheckEnabled) {
        this.bulkScanningClient = bulkScanningClient;
        this.bulkScanningCheckEnabled = bulkScanningCheckEnabled;
    }

    /**
     * Check if the given Service Request is eligible for cancellation
     * @param paymentFeeLink    Reference to the Service Request
     * @param userAuthorization the caller's user token, needed to check bulk scanning; null for an automated
     *                          cancellation, which is then ineligible unless the bulk scanning check is disabled
     * @return eligibility status including reason if not eligible
     */
    public Eligibility checkEligibility(PaymentFeeLink paymentFeeLink, String userAuthorization) {

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

        return checkEligibilityBulkScanning(paymentFeeLink, userAuthorization);
    }

    /**
     * Check bulk scanning for payments against the case that are still waiting to be allocated (BR-9, LLD 6.1).
     *
     * <p>Bulk scanning reports {@code PROCESSED} once every payment on the case has been allocated, and no status
     * when it has nothing for the case. {@code COMPLETE} (ready to allocate) and {@code INCOMPLETE} (still being
     * worked on as an exception record) both mean money may be heading for this case, so both block cancellation.</p>
     *
     * <p>Bulk scanning only allows its case search with a user token. An automated cancellation has none, so
     * it cannot be checked and is treated as ineligible. Disable the check with
     * {@code feature.cancellation.bulk-scanning-check} to allow automated cancellation without it.</p>
     */
    Eligibility checkEligibilityBulkScanning(PaymentFeeLink paymentFeeLink, String userAuthorization) {
        if (!bulkScanningCheckEnabled) {
            log.warn("Bulk scanning check disabled, so {} was not checked for unallocated bulk scan payments",
                render(paymentFeeLink));
            return Eligibility.eligible();
        }

        String ccdCaseNumber = paymentFeeLink.getCcdCaseNumber();
        if (ccdCaseNumber == null || ccdCaseNumber.isBlank()) {
            // Bulk-scanned payments are always held against a CCD case, so there is nothing to look up.
            return Eligibility.eligible();
        }

        if (userAuthorization == null || userAuthorization.isBlank()) {
            log.warn("{} ineligible for cancellation as bulk scanning cannot be checked without a user token",
                render(paymentFeeLink));
            return BULK_SCANNING_NOT_CHECKED_WITHOUT_USER_TOKEN;
        }

        Optional<PaymentStatus> bulkScanStatus = bulkScanningClient.findPaymentsForCase(ccdCaseNumber, userAuthorization)
            .map(SearchResponse::getAllPaymentsStatus);

        if (bulkScanStatus.isPresent() && bulkScanStatus.get() != PaymentStatus.PROCESSED) {
            log.info("{} ineligible for cancellation as bulk scanning has {} payments on the case",
                render(paymentFeeLink), bulkScanStatus.get());
            return UNALLOCATED_BULK_SCAN_PAYMENT_ON_CASE;
        }

        return Eligibility.eligible();
    }


    String render(PaymentFeeLink sr) {
        return "Service Request (id=%s, payment_reference=%s, ccd_case_number=%s)"
            .formatted(sr.getId(), sr.getPaymentReference(), sr.getCcdCaseNumber());
    }

}
