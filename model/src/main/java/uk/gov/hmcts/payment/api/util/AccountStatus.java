package uk.gov.hmcts.payment.api.util;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum AccountStatus {
    @JsonProperty("Active")
    ACTIVE("Active"),
    @JsonProperty("On-Hold")
    ON_HOLD("On-Hold"),
    @JsonProperty("Deleted")
    DELETED("Deleted");


    private String status;

    AccountStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return this.status;
    }
}
