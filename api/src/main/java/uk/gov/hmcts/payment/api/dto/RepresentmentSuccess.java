package uk.gov.hmcts.payment.api.dto;

public enum RepresentmentSuccess {
    Yes("Yes"),
    No("No");

    private String status;

    RepresentmentSuccess(String status) {
        this.status = status;
    }

    public String getStatus() {
        return this.status;
    }
}
