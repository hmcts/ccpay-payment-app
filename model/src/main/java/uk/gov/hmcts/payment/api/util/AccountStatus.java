package uk.gov.hmcts.payment.api.util;

public enum AccountStatus {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE");

    private String status;

    AccountStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return this.status;
    }
}
