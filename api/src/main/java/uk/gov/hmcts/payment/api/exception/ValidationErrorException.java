package uk.gov.hmcts.payment.api.exception;

import uk.gov.hmcts.payment.api.contract.exception.ValidationErrorDTO;

public class ValidationErrorException extends RuntimeException {

    private final ValidationErrorDTO errors;

    public ValidationErrorException(String message, ValidationErrorDTO errors) {
        super(message);
        this.errors = errors;
    }

    public ValidationErrorDTO getErrors() {
        return errors;
    }
}
