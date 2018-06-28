package uk.gov.hmcts.payment.api.v1.model.exceptions;

public class PaymentInformationForbidden extends Exception {
    public PaymentInformationForbidden(String message) {
        super(message);
    }
}
