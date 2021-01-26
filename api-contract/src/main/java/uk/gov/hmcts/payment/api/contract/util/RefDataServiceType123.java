package uk.gov.hmcts.payment.api.contract.util;

public enum RefDataServiceType123 {
    CMC("Specified Money Claims"),
    DIVORCE("Divorce"),
    FINREM("Financial Remedy"),
    FPL("Family Public Law"),
    PROBATE("Probate"),
    UNSPEC("Unspecified Money Claims");

    private String name;

    RefDataServiceType123(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public static RefDataServiceType123 fromString(String name) throws IllegalArgumentException {
        for (RefDataServiceType123 enumName : RefDataServiceType123.values()) {
            if (enumName.getName().equalsIgnoreCase(name)) {
                return enumName;
            }
        }
        return null;
    }
}

