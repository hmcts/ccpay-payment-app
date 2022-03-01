package uk.gov.hmcts.payment.api.dto.servicerequest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "paymentDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ServiceRequestPaymentDto {

    @JsonProperty("amount")
    @NotNull(message = "amount can't be Blank")
    @DecimalMin("0.01")
    @Positive
    @Digits(integer = 10, fraction = 2, message = "Payment amount cannot have more than 2 decimal places")
    private BigDecimal amount;

    @NotNull(message = "currency can't be Blank")
    private String currency;

    @JsonProperty("customer_reference")
    @NotEmpty(message = "customer_reference can't be Blank")
    private String customerReference;

    @JsonProperty("account_number")
    @NotEmpty(message = "account_number can't be Blank")
    private String accountNumber;

    @JsonProperty("idempotency_key")
    @NotEmpty(message = "idempotency_key can't be Blank")
    private String idempotencyKey;

    @JsonProperty("organisation_name")
    @NotEmpty(message = "organisation_name can't be Blank")
    private String organisationName;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceRequestPaymentDto that = (ServiceRequestPaymentDto) o;
        return amount.equals(that.amount) &&
            currency == that.currency &&
            customerReference.equals(that.customerReference) &&
            accountNumber.equals(that.accountNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.toString(), currency, customerReference, accountNumber);
    }

    public int hashCodeWithServiceRequestReference(String orderReference) {
        return Objects.hash(orderReference.trim(), amount.abs().toString(), currency, customerReference.trim(), accountNumber.trim());
    }

    @JsonIgnore
    @AssertFalse(message = "Invalid currency. Accepted value GBP")
    public boolean isValidCurrency() {
        String[] validCurrencys = {"GBP"};
        return currency != null && ! Arrays.asList(validCurrencys).stream().anyMatch(vm -> vm.equalsIgnoreCase(currency));
    }
}
