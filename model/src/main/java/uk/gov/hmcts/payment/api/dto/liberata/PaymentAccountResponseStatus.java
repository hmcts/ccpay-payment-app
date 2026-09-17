package uk.gov.hmcts.payment.api.dto.liberata;

public enum PaymentAccountResponseStatus {
    SUCCESS("success"),
    ERROR("error"),

    EXCEEDED_CREDIT_LIMIT("Exceeded credit limit."),
    ACCOUNT_NOT_FOUND("Account not found."),
    VALIDATION_FAILED("Validation failed."),
    ACCOUNT_NOT_ACTIVE("Account not active."),
    DUPLICATE_PAYMENT_DETECTED("Duplicate payment detected."),
    UNAUTHENTICATED("Unauthenticated.");

    private final String value;

    PaymentAccountResponseStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
