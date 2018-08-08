package uk.gov.hmcts.payment.api.contract.util;

public enum Method {

    CASH("cash"),
    CHEQUE("cheque"),
    POSTAL_ORDER("postal order"),
    CARD("card");

    Method(String type) {
        this.type = type;
    }

    String type;

    public String getType() {
        return type;
    }

}
