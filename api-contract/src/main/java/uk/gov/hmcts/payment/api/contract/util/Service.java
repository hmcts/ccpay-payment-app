package uk.gov.hmcts.payment.api.contract.util;

public enum Service {
    CMC("Civil Money Claims"),
    DIVORCE("Divorce"),
    PROBATE("Probate"),
    FINREM("Finrem"),
    DIGITAL_BAR("Digital Bar");

    private String serviceName;

    Service(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return this.serviceName;
    }
}
