package uk.gov.hmcts.payment.api.domain.service;

import java.util.List;

/**
 * The error messages returned by {@code POST /service-request/{service-request-reference}/cancel}, as plain-text
 * response bodies.
 *
 * <p>Clients can rely on these messages, so they are part of the API's contract. They are documented as Swagger
 * examples on {@code ServiceRequestController}, which is why they are compile-time constants (annotations cannot use
 * any other kind of value), and listed in Appendix A of the Service Request Cancellation LLD.</p>
 *
 * @see uk.gov.hmcts.payment.api.componenttests.SwaggerSpecTest
 * @see ServiceRequestCancellationEligibilityServiceTest
 */
public final class ServiceRequestCancellationErrors {

    /** The longest cancellation reason accepted, which matches the payment_fee_link.cancellation_reason column size. */
    public static final int MAX_CANCELLATION_REASON_LENGTH = 255;

    // 400: the request is invalid.

    public static final String INVALID_REFERENCE_FORMAT = "invalid service request reference format";

    /** Followed by the unknown field names, separated by commas. */
    public static final String UNKNOWN_FIELDS_PREFIX = "unknown field(s) in request body: ";
    public static final String MANUAL_REASON_REQUIRED = "cancellation reason needs to be supplied for manual service request cancellation";
    public static final String REASON_REQUIRED_WITH_BODY = "cancellation reason must have a value when body supplied";

    // Concatenation, not String.formatted(), keeps these compile-time constants for the Swagger annotations.
    public static final String REASON_TOO_LONG =
        "cancellation reason must not exceed " + MAX_CANCELLATION_REASON_LENGTH + " characters";

    public static final String MANUAL_CANCELLER_UNKNOWN = "could not determine id for manual service request cancellation";

    public static final String AUTOMATED_CANCELLER_UNKNOWN =
        "could not determine id for automated service request cancellation";

    // 409: the service request is not eligible for cancellation. Only the first reason found is returned.

    public static final String PAYMENT_CREATED = "payment with status created";
    public static final String PAYMENT_STARTED = "payment with status started";
    public static final String PAYMENT_SUCCESS = "payment with status success";
    public static final String PAYMENT_PENDING = "payment with status pending";
    public static final String PAYMENT_SUBMITTED = "payment with status submitted";
    public static final String UNKNOWN_PAYMENT_STATUS = "unknown payment status";
    public static final String REMISSIONS_EXISTING = "remissions existing on SR";
    public static final String PAYMENT_ALLOCATION_EXISTING = "payment allocation existing on SR";
    public static final String BULK_SCANNING_NOT_CHECKED_WITHOUT_SYSTEM_USER =
        "unable to check bulk scanning for unallocated payments as no system user is configured";
    public static final String UNALLOCATED_BULK_SCAN_PAYMENT = "unallocated bulk scan payment existing on case";

    /** Every reason a service request can be refused cancellation, in the order the checks are made. */
    public static final List<String> INELIGIBLE_REASONS = List.of(
        PAYMENT_CREATED,
        PAYMENT_STARTED,
        PAYMENT_SUCCESS,
        PAYMENT_PENDING,
        PAYMENT_SUBMITTED,
        UNKNOWN_PAYMENT_STATUS,
        REMISSIONS_EXISTING,
        PAYMENT_ALLOCATION_EXISTING,
        BULK_SCANNING_NOT_CHECKED_WITHOUT_SYSTEM_USER,
        UNALLOCATED_BULK_SCAN_PAYMENT
    );

    // 504: bulk scanning, or the IDAM login it needs, failed or did not respond.

    public static final String BULK_SCANNING_UNAVAILABLE = "unable to check bulk scanning for unallocated payments";

    private ServiceRequestCancellationErrors() {
    }
}
