package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import lombok.*;
import jakarta.validation.constraints.NotNull;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "supplementaryDetailsWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SupplementaryDetails {

    @NotNull
    private String surname;

    @NotNull
    private String caseReferenceNumber;
}
