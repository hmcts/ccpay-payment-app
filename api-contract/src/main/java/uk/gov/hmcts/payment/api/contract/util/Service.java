package uk.gov.hmcts.payment.api.contract.util;

import javax.xml.namespace.QName;

public enum Service {
    CMC("Civil Money Claims"),
    DIVORCE("Divorce"),
    PROBATE("Probate"),
    FINREM("Finrem"),
    DIGITAL_BAR("Digital Bar"),
    FPL("Family Public Law"),
    IAC("Immigration and Asylum");

    private String name;

    Service(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}


