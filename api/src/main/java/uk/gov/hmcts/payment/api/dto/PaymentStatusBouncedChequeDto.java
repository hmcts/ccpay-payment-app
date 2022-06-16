package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderMethodName = "paymentStatusBouncedChequeRequestWith")
public class PaymentStatusBouncedChequeDto {

    @NotNull(message = "Payment Reference cannot be null")
    @NotEmpty(message = "Payment Reference cannot be blank")
    private String paymentReference;
    @NotBlank
    private String failureReference;
    @NotBlank
    private String ccdCaseNumber;
    @NotBlank
    private String reason;

    @DecimalMin(value = "0.01", message = "Amount must be greater than or equal to 0.01")
    @Positive(message = "Amount must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Amount cannot have more than 2 decimal places")
    private BigDecimal amount;

    private String additionalReference;

    private Date failureEventDateTime;
}
