package uk.gov.hmcts.payment.api.dto.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import org.hibernate.validator.constraints.UniqueElements;
import uk.gov.hmcts.payment.api.contract.FeeDto;

import javax.validation.Valid;
import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "orderDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrderDto {

    @NotNull
    private String ccdCaseNumber;

    @NotNull
    private List<OrderFeeDto> fees;

    @NotBlank
    private String caseReference;

    @NotBlank
    private String caseType;

    @AssertFalse(message = "Fee code must be unique")
    private boolean isFeeCodeUnique() {
        Set<String> unique = new HashSet<>();
        return fees.stream()
            .anyMatch(p -> !unique.add(p.getCode()));
    }

    @AssertTrue(message = "ccdCaseNumber should be 16 digit")
    private boolean isValidCcdCaseNumber() {
        return (ccdCaseNumber.matches("[0-9]+") && ccdCaseNumber.length() == 16);
    }

}
