package uk.gov.hmcts.payment.api.domain.service;

import java.util.List;

/**
 * The error messages returned by {@code POST /service-request/{service-request-reference}/cancel}
 *
 * <p>These messages are written to be shown to users</p>
 *
 * @see uk.gov.hmcts.payment.api.componenttests.SwaggerSpecTest
 * @see ServiceRequestCancellationEligibilityServiceTest
 */
public final class ServiceRequestCancellationErrors {

    private ServiceRequestCancellationErrors() {}

    public static final int MAX_CANCELLATION_REASON_LENGTH = 255;

    public static final String INVALID_REFERENCE_FORMAT = "Invalid service request reference format";

    public static final String UNKNOWN_FIELDS_PREFIX = "Unknown field(s) in request body: ";
    public static final String MANUAL_REASON_REQUIRED = "Cancellation reason needs to be supplied for manual service request cancellation";
    public static final String REASON_REQUIRED_WITH_BODY = "Cancellation reason must have a value when a body is supplied";

    public static final String REASON_TOO_LONG =
        "Cancellation reason must not exceed " + MAX_CANCELLATION_REASON_LENGTH + " characters";

    public static final String PAYMENT_CREATED = "Service request has a created payment %s";
    public static final String PAYMENT_STARTED = "Service request has a started payment %s";
    public static final String PAYMENT_SUCCESS = "Service request has a successful payment %s";
    public static final String PAYMENT_PENDING = "Service request has a pending payment %s";
    public static final String PAYMENT_SUBMITTED = "Service request has a submitted payment %s";
    public static final String UNKNOWN_PAYMENT_STATUS = "Service request has an unknown status for payment %s";
    public static final String REMISSIONS_EXIST = "Service request has a remission";
    public static final String PAYMENT_ALLOCATION_EXIST = "Service request has an allocated payment";
    public static final String UNALLOCATED_BULK_SCAN_PAYMENT = "Case has an unallocated bulk scan payment";

    /** A made-up payment reference, shown in place of %s in the Swagger examples. */
    public static final String EXAMPLE_PAYMENT_REFERENCE = "RC-1234-5678-9012-3456";

    /** Every reason a service request can be refused cancellation, as given when the payment is EXAMPLE_PAYMENT_REFERENCE. */
    public static final List<String> EXAMPLE_INELIGIBLE_REASONS = List.of(
        PAYMENT_CREATED.formatted(EXAMPLE_PAYMENT_REFERENCE),
        PAYMENT_STARTED.formatted(EXAMPLE_PAYMENT_REFERENCE),
        PAYMENT_SUCCESS.formatted(EXAMPLE_PAYMENT_REFERENCE),
        PAYMENT_PENDING.formatted(EXAMPLE_PAYMENT_REFERENCE),
        PAYMENT_SUBMITTED.formatted(EXAMPLE_PAYMENT_REFERENCE),
        UNKNOWN_PAYMENT_STATUS.formatted(EXAMPLE_PAYMENT_REFERENCE),
        REMISSIONS_EXIST,
        PAYMENT_ALLOCATION_EXIST,
        UNALLOCATED_BULK_SCAN_PAYMENT
    );

    public static final String BULK_SCANNING_NOT_CONFIGURED =
        "Unable to check bulk scanning for unallocated payments as no system user is configured";

    public static final String BULK_SCANNING_UNAVAILABLE = "Unable to check bulk scanning for unallocated payments, try again later";

}
