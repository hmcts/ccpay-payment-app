package uk.gov.hmcts.payment.api.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.payment.api.domain.service.ServiceRequestCancellationEligibilityService.Eligibility;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentAllocation;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.model.Remission;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceRequestCancellationEligibilityServiceTest {

    private final ServiceRequestCancellationEligibilityService service =
        new ServiceRequestCancellationEligibilityService();

    @Test
    void eligibleHasNoReason() {
        Eligibility eligibility = Eligibility.eligible();

        assertThat(eligibility.cancellable()).isTrue();
        assertThat(eligibility.reason()).isNull();
    }

    @Test
    void ineligibleKeepsReason() {
        Eligibility eligibility = Eligibility.ineligible("Service request has a successful payment RC-1234-1234-1234");

        assertThat(eligibility.cancellable()).isFalse();
        assertThat(eligibility.reason()).isEqualTo("Service request has a successful payment RC-1234-1234-1234");
    }

    @Test
    void ineligibleRequiresReason() {
        assertThatThrownBy(() -> Eligibility.ineligible(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("a reason is required when a service request cannot be cancelled");
        assertThatThrownBy(() -> Eligibility.ineligible(" "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("a reason is required when a service request cannot be cancelled");
    }

    @Test
    void eligibleMustNotHaveReason() {
        assertThatThrownBy(() -> new Eligibility(true, "reason"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("a cancellable service request must not have a reason");
    }

    @ParameterizedTest
    @MethodSource("blockingPaymentStatuses")
    void blocksCancellationForSuccessfulOrInProgressPayments(PaymentStatus status) {
        Eligibility eligibility = service.checkEligibility(paymentFeeLink(false, payment(status)));

        assertThat(eligibility.cancellable()).isFalse();
        assertThat(eligibility.reason()).isNotBlank();
    }

    @Test
    void reportsOnlyTheFirstIneligiblePayment() {
        Eligibility eligibility = service.checkEligibility(paymentFeeLink(
            false,
            payment(status("success")),
            payment(status("created"))
        ));

        assertThat(eligibility.cancellable()).isFalse();
        assertThat(eligibility.reason()).contains("Success");
        assertThat(eligibility.reason()).doesNotContain("Created");
    }

    static Stream<Arguments> blockingPaymentStatuses() {
        return Stream.of(
            Arguments.of(status("success")),
            Arguments.of(status("pending")),
            Arguments.of(status("created")),
            Arguments.of(status("started"))
        );
    }

    @ParameterizedTest
    @MethodSource("nonBlockingPaymentStatuses")
    void allowsCancellationForFailedOrCancelledPayments(PaymentStatus status) {
        Eligibility eligibility = service.checkEligibility(paymentFeeLink(false, payment(status)));

        assertThat(eligibility).isEqualTo(Eligibility.eligible());
    }

    @Test
    void matchesPaymentStatusByNameRatherThanReferenceDataDescription() {
        PaymentStatus persistedFailedStatus = status("failed", "Invalid payment details/unsuccessful payment");

        assertThat(service.checkEligibility(paymentFeeLink(false, payment(persistedFailedStatus))))
            .isEqualTo(Eligibility.eligible());
    }

    static Stream<Arguments> nonBlockingPaymentStatuses() {
        return Stream.of(
            Arguments.of(status("failed")),
            Arguments.of(status("error")),
            Arguments.of(status("decline")),
            Arguments.of(status("cancelled"))
        );
    }

    @Test
    void allowsCancellationWhenThereAreNoPayments() {
        assertThat(service.checkEligibility(paymentFeeLink(false)))
            .isEqualTo(Eligibility.eligible());
    }

    @Test
    void blocksCancellationWhenTheServiceRequestHasARemission() {
        PaymentFeeLink paymentFeeLink = paymentFeeLink(false);
        paymentFeeLink.setRemissions(List.of(new Remission()));

        Eligibility eligibility = service.checkEligibility(paymentFeeLink);

        assertThat(eligibility.cancellable()).isFalse();
        assertThat(eligibility.reason()).isNotBlank();
    }

    @Test
    void blocksCancellationWhenAnyPaymentHasAnAllocation() {
        Payment payment = payment(status("failed"));
        payment.setPaymentAllocation(List.of(new PaymentAllocation()));

        Eligibility eligibility = service.checkEligibility(paymentFeeLink(false, payment));

        assertThat(eligibility.cancellable()).isFalse();
        assertThat(eligibility.reason()).isNotBlank();
    }

    @Test
    void alreadyCancelledServiceRequestRemainsEligibleForIdempotentCancellation() {
        assertThat(service.checkEligibility(paymentFeeLink(true)))
            .isEqualTo(Eligibility.eligible());
    }

    private static PaymentFeeLink paymentFeeLink(boolean cancelled, Payment... payments) {
        return PaymentFeeLink.paymentFeeLinkWith()
            .cancelled(cancelled)
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
        return status(name, name);
    }

    private static PaymentStatus status(String name, String description) {
        return PaymentStatus.paymentStatusWith().name(name).description(description).build();
    }

}
