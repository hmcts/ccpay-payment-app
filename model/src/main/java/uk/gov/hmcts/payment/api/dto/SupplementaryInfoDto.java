package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.validation.constraints.NotNull;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "supplementaryInfoDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SupplementaryInfoDto {

    @NotNull
    private String ccdCaseNumber;

    @NotNull
    private SupplementaryDetailsDto supplementaryDetails;
}
