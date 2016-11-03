
package uk.gov.justice.payment.api.json.external;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "status",
        "finished",
        "message",
        "code"
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class State {
    private String status;
    private Boolean finished;
    private String message;
    private String code;
}
