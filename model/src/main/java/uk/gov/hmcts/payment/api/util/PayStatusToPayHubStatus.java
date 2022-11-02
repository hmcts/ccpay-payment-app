package uk.gov.hmcts.payment.api.util;

import lombok.Getter;
import lombok.Setter;

public enum PayStatusToPayHubStatus {
    CREATED(PayStatusToPayHubStatus.INITIATED_VALUE), STARTED(PayStatusToPayHubStatus.INITIATED_VALUE),
    SUBMITTED(PayStatusToPayHubStatus.INITIATED_VALUE), SUCCESS("Success"),
    FAILED(PayStatusToPayHubStatus.FAILED_VALUE), CANCELLED(PayStatusToPayHubStatus.FAILED_VALUE),
    ERROR(PayStatusToPayHubStatus.FAILED_VALUE), PENDING("Pending"), DECLINE("Declined");

    public static final String INITIATED_VALUE = "Initiated";
    public static final String FAILED_VALUE = "Failed";

    @Getter
    @Setter
    private String mappedStatus;

    PayStatusToPayHubStatus(String status) {
        this.mappedStatus = status;
    }
}
