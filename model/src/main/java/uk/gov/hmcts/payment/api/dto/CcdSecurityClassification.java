package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum CcdSecurityClassification {
    @JsonProperty("PUBLIC")
    PUBLIC("PUBLIC"),
    @JsonProperty("PRIVATE")
    PRIVATE("PRIVATE"),
    @JsonProperty("RESTRICTED")
    RESTRICTED("RESTRICTED");

    private String securityClassification;

    CcdSecurityClassification(String status) {
        this.securityClassification = status;
    }

    public String getSecurityClassification() {
        return this.securityClassification;
    }
}
