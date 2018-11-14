package uk.gov.hmcts.payment.api.util;

public enum AccountStatus {
    Active("Active"),
    Inactive("Inactive");

    private String status;

    AccountStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return this.status;
    }
}
