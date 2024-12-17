package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "onlineCardPaymentRequestWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OnlineCardPaymentRequest {

    @JsonProperty("amount")
    @NotNull(message = "amount can't be Blank")
    @DecimalMin("0.01")
    @Positive
    @Digits(integer = 10, fraction = 2, message = "Payment amount cannot have more than 2 decimal places")
    private BigDecimal amount;

    @NotNull(message = "currency can't be Blank")
    private CurrencyCode currency;

    @NotNull(message = "language can't be Blank")
    @NotEmpty(message = "language can't be Empty")
    private String language;

    @NotNull(message = "return-url can't be Blank")
    @NotEmpty(message = "return-url can't be Empty")
    @JsonProperty("return-url")
    private String returnUrl;
}
