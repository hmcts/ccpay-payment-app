package uk.gov.hmcts.payment.api.util;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum AccountStatus {
    @JsonProperty("Active")
    ACTIVE("Active"),
    @JsonProperty("Inactive")
    INACTIVE("Inactive");

    private String status;

    AccountStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return this.status;
    }
}
