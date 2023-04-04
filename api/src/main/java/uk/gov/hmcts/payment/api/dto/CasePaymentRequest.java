package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import javax.validation.constraints.NotBlank;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "casePaymentRequestWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CasePaymentRequest {

    @NotBlank(message = "action should not be blank")
    private String action;

    @NotBlank(message = "responsibleParty should not be blank")
    private String responsibleParty;
}
