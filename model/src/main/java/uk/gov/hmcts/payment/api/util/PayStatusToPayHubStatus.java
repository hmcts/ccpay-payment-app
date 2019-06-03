package uk.gov.hmcts.payment.api.util;

import lombok.Getter;
import lombok.Setter;

public enum PayStatusToPayHubStatus {
    created("Initiated"), started("Initiated"), submitted("Initiated"), success("Success"), failed("Failed"), cancelled("Failed"), error("Failed"), pending("Pending");

    @Getter
    @Setter
    private String mappedStatus;

    PayStatusToPayHubStatus(String status) {
        this.mappedStatus = status;
    }
}
