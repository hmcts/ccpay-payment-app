
package uk.gov.hmcts.payment.api.external.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class State {
    private String status;
    private Boolean finished;
    private String message;
    private String code;
}
