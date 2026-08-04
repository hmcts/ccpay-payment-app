package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import lombok.*;
import jakarta.validation.constraints.NotNull;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "supplementaryInfoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SupplementaryInfo {

    @NotNull
    private String ccdCaseNumber;

    @NotNull
    private SupplementaryDetails supplementaryDetails;
}
