package uk.gov.hmcts.payment.api.contract.util;

public enum PaymentChannels {
    TELEPHONY("telephony");

    private String code;

    PaymentChannels(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
