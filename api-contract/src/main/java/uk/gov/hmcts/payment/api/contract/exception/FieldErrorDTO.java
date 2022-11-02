package uk.gov.hmcts.payment.api.contract.exception;

import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

@ToString
@NoArgsConstructor
public class FieldErrorDTO implements Serializable {

    private static final long serialVersionUID = 1905122041950258976L;

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
