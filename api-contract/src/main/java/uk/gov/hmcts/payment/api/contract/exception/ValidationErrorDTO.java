package uk.gov.hmcts.payment.api.contract.exception;

import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@ToString
@NoArgsConstructor
public class ValidationErrorDTO {

    private List<FieldErrorDTO> fieldErrors = new ArrayList<>();

    public void addFieldError(String path, String message) {
        FieldErrorDTO error = new FieldErrorDTO(path, message);
        fieldErrors.add(error);
    }

    public List<FieldErrorDTO> getFieldErrors() {
        return fieldErrors;
    }

    public boolean hasErrors() {
        return !fieldErrors.isEmpty();
    }

    public class ValidationErrorException extends RuntimeException {
        private final ValidationErrorDTO validationErrorDTO;

        public ValidationErrorException(String message, ValidationErrorDTO validationErrorDTO) {
            super(message);
            this.validationErrorDTO = validationErrorDTO;
        }

        public ValidationErrorDTO getValidationErrorDTO() {
            return validationErrorDTO;
        }
    }
    public class FieldError {
        private final String field;
        private final String message;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }


    }


}
