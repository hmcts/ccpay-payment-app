package uk.gov.hmcts.payment.api.dto;

public enum RepresentmentStatus {
    YES("Yes"),
    NO("No");

    private String status;

    RepresentmentStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return this.status;
    }
}
