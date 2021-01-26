package uk.gov.hmcts.payment.api.contract.util;

public enum Service123 {
    CMC("Civil Money Claims"),
    DIVORCE("Divorce"),
    PROBATE("Probate"),
    FINREM("Finrem"),
    DIGITAL_BAR("Digital Bar"),
    FPL("Family Public Law"),
    IAC("Immigration and Asylum Appeals"),
    UNSPEC("Unspecified Claim");

    private String name;

    Service123(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
