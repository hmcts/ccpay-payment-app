package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.payment.api.model.Remission;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderMethodName = "createPaymentRecordRequestDtoWith")
public class RemissionRequest {
    @NotEmpty
    private String hwfReference;

    @NotNull
    @DecimalMin("0.01")
    @Positive
    @Digits(integer = 10, fraction = 2, message = "Amount cannot have more than 2 decimal places")
    private BigDecimal hwfAmount;

    private String beneficiaryName;

    @NotEmpty
    private String ccdCaseNumber;

    private String caseReference;

    private String paymentGroupReference;

    public Remission toRemission() {
        return Remission.remissionWith()
            .hwfReference(hwfReference)
            .hwfAmount(hwfAmount)
            .beneficiaryName(beneficiaryName)
            .ccdCaseNumber(ccdCaseNumber)
            .caseReference(caseReference)
            .paymentGroupReference(paymentGroupReference)
            .build();
    }
}
