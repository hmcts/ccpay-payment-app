package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.payment.api.contract.FeeDto;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderMethodName = "createRemissionRequestWith")
public class RemissionRequest {
    @NotEmpty
    private String hwfReference;

    @NotNull
    @DecimalMin("0.01")
    @Positive
    @Digits(integer = 10, fraction = 2, message = "Amount cannot have more than 2 decimal places")
    private BigDecimal hwfAmount;

    private String beneficiaryName;

    private String ccdCaseNumber;

    private String caseReference;

    private String paymentGroupReference;

    @NotNull
    @Valid
    private FeeDto fee;

    @NotNull
    private String siteId;

    @AssertFalse(message = "Hwf amount cannot be greater than calculated amount.")
    private boolean isHwfAmountInvalid() {
        return (hwfAmount != null && fee != null) &&
            (hwfAmount.compareTo(fee.getCalculatedAmount()) == 1);
    }

    @AssertFalse(message = "Either ccdCaseNumber or caseReference is required.")
    private boolean isEitherOneRequired() {
        return (ccdCaseNumber == null && caseReference == null);
    }

}
