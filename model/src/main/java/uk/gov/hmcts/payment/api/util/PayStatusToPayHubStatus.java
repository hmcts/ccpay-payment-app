package uk.gov.hmcts.payment.api.util;

public enum PayStatusToPayHubStatus {
    created("Initiated"), started("Initiated"), submitted("Initiated"), success("Success"), failed("Failed"), cancelled("Failed"), error("Failed"), pending("Pending");

    public String mappedStatus;

    PayStatusToPayHubStatus(String status) {
        this.mappedStatus = status;
    }

    String getMappedStatus() {
        return mappedStatus;
    }

}
