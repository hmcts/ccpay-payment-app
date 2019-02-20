package uk.gov.hmcts.payment.api.contract.util;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Service {
    @JsonProperty("CMC")
    CMC("Civil Money Claims"),
    DIVORCE("Divorce"),
    PROBATE("Probate"),
    FINREM("Finrem"),
    DIGITAL_BAR("Digital Bar");

    private String name;

    Service(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
