package uk.gov.hmcts.payment.api.jpaaudit.listner;

public enum Action {

    INSERTED("INSERTED"),
    UPDATED("UPDATED"),
    DELETED("DELETED");
    private final String name;
    private Action(String value) {
        this.name = value;
    }
    public String value() {
        return this.name;
    }
    @Override
    public String toString() {
        return name;
    }
}
