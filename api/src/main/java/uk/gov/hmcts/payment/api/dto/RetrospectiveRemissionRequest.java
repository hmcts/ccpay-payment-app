package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import uk.gov.hmcts.payment.api.model.ContactDetails;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@With
@JsonInclude(NON_NULL)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderMethodName = "retrospectiveRemissionRequestWith")
public class RetrospectiveRemissionRequest {

    @NotEmpty
    @JsonProperty("remissionReference")
    private String remissionReference;

    @NotNull(message = "Contact Details cannot be null")
    private ContactDetails contactDetails;
}
