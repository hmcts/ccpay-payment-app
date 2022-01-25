package uk.gov.hmcts.payment.api.dto.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "serviceRequestDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ServiceRequestDto {

    @NotNull
    @Pattern(regexp = "^[0-9]{16}",message = "ccd_case_number should be 16 digit")
    private String ccdCaseNumber;

    @NotEmpty
    private List<@Valid ServiceRequestFeeDto> fees;

    @NotNull
    private @Valid CasePaymentRequest casePaymentRequest;

    @NotBlank
    private String caseReference;

    @NotBlank
    private String callBackUrl;

    @NotBlank
    private String hmctsOrgId;

    @AssertFalse(message = "Fee code cannot be duplicated")
    private boolean isFeeCodeUnique() {
        Set<String> unique = new HashSet<>();

        if(fees!=null)
        return fees.stream()
            .anyMatch(p -> !unique.add(p.getCode()));

        else return false;
    }

}
