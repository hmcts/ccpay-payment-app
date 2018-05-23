package uk.gov.hmcts.payment.api.contract.exception;

import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@NoArgsConstructor
public class FieldErrorDTO {

    private String field;

    private String message;

    public FieldErrorDTO(String field, String message) {
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
