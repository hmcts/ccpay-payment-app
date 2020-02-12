package uk.gov.hmcts.payment.api.contract.util;

public enum Language {
    CY("cy");

    private String language;

    Language(String language) {
        this.language = language;
    }

    public String getLanguage() {
        return this.language;
    }
}
