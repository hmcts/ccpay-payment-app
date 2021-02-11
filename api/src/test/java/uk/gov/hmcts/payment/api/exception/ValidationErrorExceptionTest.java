package uk.gov.hmcts.payment.api.exception;

import org.junit.Test;
import uk.gov.hmcts.payment.api.contract.exception.FieldErrorDTO;
import uk.gov.hmcts.payment.api.contract.exception.ValidationErrorDTO;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ValidationErrorExceptionTest {
    private void throwException(){

        ValidationErrorDTO errors = new ValidationErrorDTO();
        errors.addFieldError("key","message");
        throw new ValidationErrorException("validation error",errors);
    }

    @Test(expected = ValidationErrorException.class)
    public void checkingIfExceptionThrown(){
        throwException();
    }

    @Test
    public void checkingMessageThrownByException() {
        try {
            throwException();
        } catch (ValidationErrorException e) {
            ValidationErrorDTO errors = e.getErrors();
            assertEquals("key", errors.getFieldErrors().get(0).getField());
        }

    }
}
