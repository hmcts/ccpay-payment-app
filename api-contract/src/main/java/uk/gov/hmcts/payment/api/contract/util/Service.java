package uk.gov.hmcts.payment.api.contract.util;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Service {
    CIVIL_MONEY_CLAIMS("Civil Money Claims"),
    DIVORCE("Divorce"),
    PROBATE("Probate");

    private String name;

    Service(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
