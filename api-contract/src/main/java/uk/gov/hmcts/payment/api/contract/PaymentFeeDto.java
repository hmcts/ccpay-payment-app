package uk.gov.hmcts.payment.api.contract;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.launchdarkly.shaded.org.checkerframework.checker.index.qual.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import java.math.BigDecimal;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@SuperBuilder(builderMethodName = "paymentFeeDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PaymentFeeDto {
    private Integer id;

    @NotNull
    @Digits(integer = 10, fraction = 2, message = "Fee calculated amount cannot have more than 2 decimal places")
    private BigDecimal calculatedAmount;

    @NotEmpty
    private String code;

    @Digits(integer = 10, fraction = 2, message = "Net amount cannot have more than 2 decimal places")
    private BigDecimal netAmount;

    @NotEmpty
    private String version;

    @Positive
    private Integer volume;

    private String ccdCaseNumber;

    private String reference;

}
