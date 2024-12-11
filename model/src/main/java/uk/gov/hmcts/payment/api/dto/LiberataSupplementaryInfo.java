package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "liberataSupplementaryInfoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LiberataSupplementaryInfo {

    @NotNull
    private String ccdCaseNumber;

    @NotNull
    @JsonProperty("supplementary_details")
    private LiberataSupplementaryDetails supplementaryDetails;
}
