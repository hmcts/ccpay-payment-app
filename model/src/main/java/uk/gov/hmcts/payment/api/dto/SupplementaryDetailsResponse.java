package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import java.util.List;
import lombok.*;
import jakarta.validation.constraints.NotNull;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "supplementaryDetailsResponseWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SupplementaryDetailsResponse {

    @NotNull
    private List<SupplementaryInfo> supplementaryInfo;

    @NotNull
    private MissingSupplementaryInfo missingSupplementaryInfo;
}




