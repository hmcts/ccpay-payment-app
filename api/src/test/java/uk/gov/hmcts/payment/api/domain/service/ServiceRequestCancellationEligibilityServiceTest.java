package uk.gov.hmcts.payment.api.domain.service;

import org.assertj.core.api.AbstractAssert;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.payment.api.domain.service.ServiceRequestCancellationEligibilityService.Eligibility;
import uk.gov.hmcts.payment.api.dto.SearchResponse;
import uk.gov.hmcts.payment.api.exception.BulkScanningServiceUnavailableException;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentAllocation;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.model.Remission;
import uk.gov.hmcts.payment.api.service.BulkScanningClient;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @author jhilling
 */
class ServiceRequestCancellationEligibilityServiceTest {

    private static final String CCD_CASE_NUMBER = "1234567890123456";
    private static final String USER_TOKEN = "Bearer user-token";

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

    // Mockito returns Optional.empty() by default, so bulk scanning reports nothing unless a test stubs it.
    private final BulkScanningClient bulkScanningClient = mock(BulkScanningClient.class);

    private final ServiceRequestCancellationEligibilityService service =
        new ServiceRequestCancellationEligibilityService(bulkScanningClient, true);


    EligibilityAssert assertThatEligibility(Eligibility actual) {
        return new EligibilityAssert(actual);
    }

    EligibilityAssert assertThatSR(PaymentFeeLink pfl) {
        return new EligibilityAssert(service.checkEligibility(pfl, USER_TOKEN));
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

    @Test
    void blocksCancellationWhenBulkScanningHasPaymentsReadyToAllocate() {
        givenBulkScanningReturns(uk.gov.hmcts.payment.api.dto.PaymentStatus.COMPLETE);

        assertThatSR(paymentFeeLinkForCase(CCD_CASE_NUMBER)).isNotCancellable()
            .withReason("unallocated bulk scan payment existing on case");
    }

    @Test
    void blocksCancellationWhenBulkScanningHasIncompletePayments() {
        givenBulkScanningReturns(uk.gov.hmcts.payment.api.dto.PaymentStatus.INCOMPLETE);

        assertThatSR(paymentFeeLinkForCase(CCD_CASE_NUMBER)).isNotCancellable()
            .withReason("unallocated bulk scan payment existing on case");
    }

    @Test
    void allowsCancellationWhenAllBulkScanPaymentsAreProcessed() {
        givenBulkScanningReturns(uk.gov.hmcts.payment.api.dto.PaymentStatus.PROCESSED);

        assertThatSR(paymentFeeLinkForCase(CCD_CASE_NUMBER)).isCancellable();
    }

    @Test
    void allowsCancellationWhenBulkScanningHasNothingForTheCase() {
        // Bulk scanning returns 200 with no status when it has no envelopes for the case.
        givenBulkScanningReturns(null);
        assertThatSR(paymentFeeLinkForCase(CCD_CASE_NUMBER)).isCancellable();

        // The client returns empty when bulk scanning responds with 404.
        when(bulkScanningClient.findPaymentsForCase(CCD_CASE_NUMBER, USER_TOKEN)).thenReturn(Optional.empty());
        assertThatSR(paymentFeeLinkForCase(CCD_CASE_NUMBER)).isCancellable();
    }

    @Test
    void skipsBulkScanningWhenTheServiceRequestHasNoCaseNumber() {
        assertThatSR(paymentFeeLinkForCase(null)).isCancellable();
        assertThatSR(paymentFeeLinkForCase(" ")).isCancellable();

        verifyNoInteractions(bulkScanningClient);
    }

    @Test
    void skipsBulkScanningWhenALocalCheckHasAlreadyFailed() {
        PaymentFeeLink paymentFeeLink = paymentFeeLinkForCase(CCD_CASE_NUMBER);
        paymentFeeLink.setPayments(List.of(payment(status("success"))));

        assertThatSR(paymentFeeLink).isNotCancellable();
        verifyNoInteractions(bulkScanningClient);
    }

    @Test
    void skipsBulkScanningForAnAlreadyCancelledServiceRequest() {
        PaymentFeeLink paymentFeeLink = paymentFeeLinkForCase(CCD_CASE_NUMBER);
        paymentFeeLink.setCancelled(true);

        assertThatSR(paymentFeeLink).isCancellable();
        verifyNoInteractions(bulkScanningClient);
    }

    @Test
    void skipsBulkScanningWhenTheCheckIsDisabled() {
        ServiceRequestCancellationEligibilityService serviceWithCheckDisabled =
            new ServiceRequestCancellationEligibilityService(bulkScanningClient, false);
        givenBulkScanningReturns(uk.gov.hmcts.payment.api.dto.PaymentStatus.COMPLETE);

        assertThatEligibility(serviceWithCheckDisabled.checkEligibility(paymentFeeLinkForCase(CCD_CASE_NUMBER), USER_TOKEN))
            .isCancellable();
        verifyNoInteractions(bulkScanningClient);
    }

    @Test
    void blocksAnAutomatedCancellationWithoutAUserTokenAsBulkScanningCannotBeChecked() {
        assertThatEligibility(service.checkEligibility(paymentFeeLinkForCase(CCD_CASE_NUMBER), null)).isNotCancellable()
            .withReason("unable to check bulk scanning for unallocated payments without a user token");
        assertThatEligibility(service.checkEligibility(paymentFeeLinkForCase(CCD_CASE_NUMBER), " ")).isNotCancellable()
            .withReason("unable to check bulk scanning for unallocated payments without a user token");
        verifyNoInteractions(bulkScanningClient);
    }

    @Test
    void allowsAnAutomatedCancellationWithoutAUserTokenWhenTheCheckIsDisabled() {
        ServiceRequestCancellationEligibilityService serviceWithCheckDisabled =
            new ServiceRequestCancellationEligibilityService(bulkScanningClient, false);

        assertThatEligibility(serviceWithCheckDisabled.checkEligibility(paymentFeeLinkForCase(CCD_CASE_NUMBER), null))
            .isCancellable();
        verifyNoInteractions(bulkScanningClient);
    }

    @Test
    void allowsAnAutomatedCancellationWithoutAUserTokenWhenThereIsNoCaseNumber() {
        assertThatEligibility(service.checkEligibility(paymentFeeLinkForCase(null), null)).isCancellable();
        verifyNoInteractions(bulkScanningClient);
    }

    @Test
    void propagatesBulkScanningFailure() {
        BulkScanningServiceUnavailableException failure =
            new BulkScanningServiceUnavailableException("unable to check bulk scanning for unallocated payments", null);
        when(bulkScanningClient.findPaymentsForCase(CCD_CASE_NUMBER, USER_TOKEN)).thenThrow(failure);

        assertThatThrownBy(() -> service.checkEligibility(paymentFeeLinkForCase(CCD_CASE_NUMBER), USER_TOKEN))
            .isSameAs(failure);
    }

    private void givenBulkScanningReturns(uk.gov.hmcts.payment.api.dto.PaymentStatus status) {
        when(bulkScanningClient.findPaymentsForCase(CCD_CASE_NUMBER, USER_TOKEN))
            .thenReturn(Optional.of(SearchResponse.searchResponseWith().allPaymentsStatus(status).build()));
    }

    private static PaymentFeeLink paymentFeeLinkForCase(String ccdCaseNumber) {
        PaymentFeeLink paymentFeeLink = paymentFeeLink();
        paymentFeeLink.setCcdCaseNumber(ccdCaseNumber);
        return paymentFeeLink;
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
