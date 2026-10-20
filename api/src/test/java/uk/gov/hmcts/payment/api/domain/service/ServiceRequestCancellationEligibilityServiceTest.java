package uk.gov.hmcts.payment.api.domain.service;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.payment.api.domain.service.ServiceRequestCancellationEligibilityService.Eligibility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceRequestCancellationEligibilityServiceTest {

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

}
