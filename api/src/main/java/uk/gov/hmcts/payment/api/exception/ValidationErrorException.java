package uk.gov.hmcts.payment.api.exception;

import uk.gov.hmcts.payment.api.contract.exception.ValidationErrorDTO;

import java.io.Serial;
import java.io.Serializable;

public class ValidationErrorException extends RuntimeException implements Serializable {

    @Serial
    private static final long serialVersionUID = 1905122041950251207L;

    private final ValidationErrorDTO errors;

    public ValidationErrorException(String message, ValidationErrorDTO errors) {
        super(message);
        this.errors = errors;
    }

    public ValidationErrorDTO getErrors() {
        return errors;
    }
}
