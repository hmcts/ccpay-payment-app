package uk.gov.hmcts.payment.api.contract.util;

public enum Method {

    CASH("cash"),
    CHEQUE("cheque");

    Method(String type) {
        this.type = type;
    }

    String type;

    public String getType() {
        return type;
    }

}
