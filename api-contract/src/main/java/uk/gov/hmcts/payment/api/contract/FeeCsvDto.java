package uk.gov.hmcts.payment.api.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Builder(builderMethodName = "feeCsvDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class FeeCsvDto {

    @NotEmpty
    private String code;

    @NotEmpty
    private String version;

    @NotNull
    @Digits(integer = 10, fraction = 2, message = "Fee calculated amount cannot have more than 2 decimal places")
    private BigDecimal calculatedAmount;

    private String memoLine;

    private String naturalAccountCode;

}
