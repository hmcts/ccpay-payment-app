package uk.gov.hmcts.payment.api.domain.service;

import org.assertj.core.api.AbstractAssert;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.payment.api.domain.service.ServiceRequestCancellationEligibilityService.Eligibility;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentAllocation;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.model.Remission;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author jhilling
 */
class ServiceRequestCancellationEligibilityServiceTest {

    static class EligibilityAssert extends AbstractAssert<EligibilityAssert, Eligibility> {

        private EligibilityAssert(Eligibility actual) {
            super(actual, EligibilityAssert.class);
        }

        void isCancellable() {
            isNotNull();
            if (!actual.cancellable()) {
                failWithMessage("Expected eligibility to be cancellable, but it was not: %s", actual.reason());
            }
            if (actual.reason() != null) {
                failWithMessage("Expected reason to be null");
            }
        }

        EligibilityAssert isNotCancellable() {
            isNotNull();
            if (actual.cancellable()) {
                failWithMessage("Expected eligibility not to be cancellable, but it was");
            }
            hasReason();
            return this;
        }

        void hasReason() {
            isNotNull();
            if (actual.reason() == null || actual.reason().isBlank()) {
                failWithMessage("Expected reason to not be empty");
            }
        }

        void withReason(String expected) {
            isNotNull();
            hasReason();
            if (!Objects.equals(actual.reason(), expected)) {
                failWithMessage("Expected reason to be <%s> but was <%s>", expected, actual.reason());
            }
        }
    }

    private final ServiceRequestCancellationEligibilityService service =
        new ServiceRequestCancellationEligibilityService();


    EligibilityAssert assertThatEligibility(Eligibility actual) {
        return new EligibilityAssert(actual);
    }

    EligibilityAssert assertThatSR(PaymentFeeLink pfl) {
        return new EligibilityAssert(service.checkEligibility(pfl));
    }

    @Test
    void eligibleHasNoReason() {
        assertThatEligibility(Eligibility.eligible())
            .isCancellable();
    }

    @Test
    void ineligibleKeepsReason() {
        assertThatEligibility(Eligibility.ineligible("Service request has a successful payment RC-1234-1234-1234"))
            .isNotCancellable()
            .withReason("Service request has a successful payment RC-1234-1234-1234");
    }

    @Test
    void ineligibleRequiresReason() {
        assertThatIllegalArgumentException().isThrownBy(() -> Eligibility.ineligible(null))
            .withMessage("a reason is required when a service request cannot be cancelled");

        assertThatIllegalArgumentException().isThrownBy(() -> Eligibility.ineligible(" "))
            .withMessage("a reason is required when a service request cannot be cancelled");
    }

    @Test
    void eligibleMustNotHaveReason() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Eligibility(true, "reason"))
            .withMessage("a cancellable service request must not have a reason");
    }

    @Test
    void reportsOnlyTheFirstIneligiblePayment() {
        assertThatSR(paymentFeeLink(payment(status("success")), payment(status("created"))))
            .isNotCancellable()
            .withReason("payment with status success");
    }

    @Test
    void blocksCancellationForSuccessfulOrInProgressPayments() {

        assertThatSR(paymentFeeLink(payment(status("success")))).isNotCancellable()
            .withReason("payment with status success");

        assertThatSR(paymentFeeLink(payment(status("pending")))).isNotCancellable()
            .withReason("payment with status pending");

        assertThatSR(paymentFeeLink(payment(status("created")))).isNotCancellable()
            .withReason("payment with status created");

        assertThatSR(paymentFeeLink(payment(status("started")))).isNotCancellable()
            .withReason("payment with status started");

        assertThatSR(paymentFeeLink(payment(status("submitted")))).isNotCancellable()
            .withReason("payment with status submitted");
    }


    @Test
    void blocksCancellationForAnUnknownPaymentStatus() {
        assertThatSR(paymentFeeLink(payment(status("other"))))
            .isNotCancellable()
            .withReason("unknown payment status");
    }

    @Test
    void matchesPaymentStatusByNameRatherThanReferenceDataDescription() {
        assertThatSR(paymentFeeLink(
            payment(PaymentStatus.paymentStatusWith().name("failed").description("Invalid payment details/unsuccessful payment").build())))
            .isCancellable();
    }

    @Test
    void allowsCancellationForFailedOrCancelledPayments() {
        List.of("failed", "error", "decline", "cancelled").forEach(status -> {
            assertThatSR(paymentFeeLink(payment(status(status))))
                .isCancellable();
        });
    }

    @Test
    void allowsCancellationWhenThereAreNoPayments() {
        assertThatSR(paymentFeeLink()).isCancellable();
    }

    @Test
    void blocksCancellationWhenTheServiceRequestHasARemission() {
        assertThatSR(PaymentFeeLink.paymentFeeLinkWith().remissions(List.of(new Remission())).build())
            .isNotCancellable()
            .withReason("remissions existing on SR");
    }

    @Test
    void blocksCancellationWhenAnyPaymentHasAnAllocation() {

        Payment payment = Payment.paymentWith()
            .paymentStatus(status("failed"))
            .paymentAllocation(List.of(new PaymentAllocation()))
            .build();

        assertThatSR(paymentFeeLink(payment)).isNotCancellable()
            .withReason("payment allocation existing on SR");
    }

    @Test
    void alreadyCancelledServiceRequestRemainsEligibleForIdempotentCancellation() {
        assertThatSR(PaymentFeeLink.paymentFeeLinkWith().cancelled(true).build())
            .isCancellable();
    }

    private static PaymentFeeLink paymentFeeLink(Payment... payments) {
        return PaymentFeeLink.paymentFeeLinkWith()
            .cancelled(false)
            .payments(List.of(payments))
            .remissions(List.of())
            .build();
    }

    private static Payment payment(PaymentStatus status) {
        return Payment.paymentWith()
            .paymentStatus(status)
            .paymentAllocation(List.of())
            .build();
    }

    private static PaymentStatus status(String name) {
        return PaymentStatus.paymentStatusWith().name(name).description(name).build();
    }
}
