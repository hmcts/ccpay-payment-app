package uk.gov.hmcts.payment.api.dto.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;

import javax.validation.constraints.*;
import java.math.BigDecimal;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "paymentDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrderPaymentDto {

    @NotNull
    @DecimalMin("0.01")
    @Positive
    @Digits(integer = 10, fraction = 2, message = "Payment amount cannot have more than 2 decimal places")
    private BigDecimal amount;

    @NotEmpty
    private String description;

    private String ccdCaseNumber;

    private String caseReference;

    @NotNull
    private String service;

    private CurrencyCode currency;

    @NotEmpty
    private String customerReference;

    @NotEmpty
    private String organisationName;

    @NotEmpty
    private String accountNumber;
}
