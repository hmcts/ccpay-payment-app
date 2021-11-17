package uk.gov.hmcts.payment.api.util;

import lombok.Getter;
import lombok.Setter;

public enum PayStatusToPayHubStatus {
    created("Initiated"), started("Initiated"), submitted("Initiated"), success("Success"), failed("Failed"), cancelled("Cancelled"), error("Failed"), pending("Pending"), decline("Declined");

    @Getter
    @Setter
    private String mappedStatus;

    PayStatusToPayHubStatus(String status) {
        this.mappedStatus = status;
    }
}
