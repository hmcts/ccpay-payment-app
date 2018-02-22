package uk.gov.hmcts.payment.api.util;

public enum PayStatusToPayHubStatus {
    created("Initiated"), started("Initiated"), submitted("Initiated"), success("Success"), failed("Failed"), cancelled("Failed"), error("Failed");

    public String mapedStatus;

    PayStatusToPayHubStatus(String status) {
        this.mapedStatus = status;
    }

    String getMapedStatus() {
        return mapedStatus;
    }

}
