package uk.gov.hmcts.payment.api.domain.service;

import java.util.List;

/**
 * The error messages returned by {@code POST /service-request/{service-request-reference}/cancel}, as plain-text
 * response bodies.
 *
 * <p>These messages are written to be shown to users, and their wording may change. Clients should act on the HTTP
 * status, not match the text. Some may match it anyway, so treat any change as a change to the API: each message is
 * documented as a Swagger example on {@code ServiceRequestController}, which is why they are compile-time constants
 * (annotations cannot use any other kind of value), and listed in Appendix A of the Service Request Cancellation
 * LLD.</p>
 *
 * @see uk.gov.hmcts.payment.api.componenttests.SwaggerSpecTest
 * @see ServiceRequestCancellationEligibilityServiceTest
 */
public final class ServiceRequestCancellationErrors {

    private ServiceRequestCancellationErrors() {}

    /** The longest cancellation reason accepted, which matches the payment_fee_link.cancellation_reason column size. */
    public static final int MAX_CANCELLATION_REASON_LENGTH = 255;

    // 400: the request is invalid.

    public static final String INVALID_REFERENCE_FORMAT = "Invalid service request reference format";

    /** Followed by the unknown field names, separated by commas. */
    public static final String UNKNOWN_FIELDS_PREFIX = "Unknown field(s) in request body: ";
    public static final String MANUAL_REASON_REQUIRED = "Cancellation reason needs to be supplied for manual service request cancellation";
    public static final String REASON_REQUIRED_WITH_BODY = "Cancellation reason must have a value when a body is supplied";

    // Built with + rather than String.formatted(), so that it stays a compile-time constant for the Swagger annotations.
    public static final String REASON_TOO_LONG =
        "Cancellation reason must not exceed " + MAX_CANCELLATION_REASON_LENGTH + " characters";

    public static final String MANUAL_CANCELLER_UNKNOWN = "Could not determine ID for manual service request cancellation";

    public static final String AUTOMATED_CANCELLER_UNKNOWN =
        "Could not determine ID for automated service request cancellation";

    // 409: the service request is not eligible for cancellation. Only one reason is returned, and which one is not part of the contract.

    public static final String PAYMENT_CREATED = "Service request has a created payment %s";
    public static final String PAYMENT_STARTED = "Service request has a started payment %s";
    public static final String PAYMENT_SUCCESS = "Service request has a successful payment %s";
    public static final String PAYMENT_PENDING = "Service request has a pending payment %s";
    public static final String PAYMENT_SUBMITTED = "Service request has a submitted payment %s";
    public static final String UNKNOWN_PAYMENT_STATUS = "Service request has an unknown status for payment %s";
    public static final String REMISSIONS_EXIST = "Service request has a remission";
    public static final String PAYMENT_ALLOCATION_EXIST = "Service request has an allocated payment";
    public static final String BULK_SCANNING_NOT_CHECKED_WITHOUT_SYSTEM_USER =
        "Unable to check bulk scanning for unallocated payments as no system user is configured";
    public static final String UNALLOCATED_BULK_SCAN_PAYMENT = "Case has an unallocated bulk scan payment";

    /** Every reason a service request can be refused cancellation. */
    public static final List<String> INELIGIBLE_REASONS = List.of(
        PAYMENT_CREATED,
        PAYMENT_STARTED,
        PAYMENT_SUCCESS,
        PAYMENT_PENDING,
        PAYMENT_SUBMITTED,
        UNKNOWN_PAYMENT_STATUS,
        REMISSIONS_EXIST,
        PAYMENT_ALLOCATION_EXIST,
        BULK_SCANNING_NOT_CHECKED_WITHOUT_SYSTEM_USER,
        UNALLOCATED_BULK_SCAN_PAYMENT
    );

    // 504: bulk scanning, or the IDAM login it needs, failed or did not respond.

    public static final String BULK_SCANNING_UNAVAILABLE = "Unable to check bulk scanning for unallocated payments, try again later";

}
