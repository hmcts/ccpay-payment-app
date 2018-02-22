package uk.gov.hmcts.payment.api.util;

public enum GovPayStatusToPayHubStatus {
    created("Initiated"), started("Initiated"), submitted("Initiated"), success("Success"), failed("Failed"), cancelled("Failed"), error("Failed");

    public String mapedStatus;

    GovPayStatusToPayHubStatus(String status) {
        this.mapedStatus = status;
    }

    String getMapedStatus() {
        return mapedStatus;
    }

}
