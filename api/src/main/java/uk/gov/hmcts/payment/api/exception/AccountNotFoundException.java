package uk.gov.hmcts.payment.api.exception;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException (String message) {
        System.out.print(message)
        super(message);
    }
}
