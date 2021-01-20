package uk.gov.hmcts.payment.api.contract.util;

import java.util.Arrays;

public enum RefDataServiceType {
    CMC("Specified Money Claims"),
    DIVORCE("Divorce"),
    FINREM("Financial Remedy"),
    FPL("Family Public Law"),
    PROBATE("Probate"),
    UNSPEC("Unspecified Money Claims");

    private String name;

    RefDataServiceType(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public static RefDataServiceType fromString(String name) throws IllegalArgumentException {
        for (RefDataServiceType enumName : RefDataServiceType.values()) {
            if (enumName.getName().equalsIgnoreCase(name)) {
                return enumName;
            }
        }
        return null;
    }
}

