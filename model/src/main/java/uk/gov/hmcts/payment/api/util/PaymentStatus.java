package uk.gov.hmcts.payment.api.util;

public enum PaymentStatus {

    CREATED("created"),
    SUCCESS("success"),
    FAILED("failed"),
    CANCELLED("cancelled"),
    ERROR("error"),
    SUBMITTED("submitted"),
    STARTED("started"),
    PENDING("pending"),
    DECLINE("decline");

    String type;

    PaymentStatus(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
