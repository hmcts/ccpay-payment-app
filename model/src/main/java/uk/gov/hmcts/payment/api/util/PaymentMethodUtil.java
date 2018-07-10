package uk.gov.hmcts.payment.api.util;

public enum PaymentMethodUtil {

    ALL("all"),
    CARD("card"),
    PBA("payment by account");

    String type;

    PaymentMethodUtil(String type) {
        this.type = type;
    }

    public String getType() {
         return type;
    }
}
