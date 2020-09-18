package uk.gov.hmcts.payment.api.contract.util;

public enum PaymentProviders {
    PCIPAL("pci pal");

    private String code;

    PaymentProviders(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
