package uk.gov.hmcts.payment.api.contract;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;
import org.hibernate.validator.constraints.NotEmpty;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.contract.util.Service;

import javax.validation.Valid;
import javax.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderMethodName = "createCardPaymentRequestDtoWith")
@Wither
public class CardPaymentRequest {

    @NotNull
    @Min(1)
    @Digits(integer = 10, fraction = 2, message = "Payment amount cannot have more than 2 decimal places")
    private BigDecimal amount;

    @NotEmpty
    private String description;

    private String ccdCaseNumber;

    private String caseReference;

    private Service service;

    private CurrencyCode currency;

    @NotEmpty
    @JsonProperty("site_id")
    private String siteId;

    @NotEmpty
    @Valid
    private List<FeeDto> fee;

    @AssertFalse(message = "Either ccdCaseNumber or caseReference is required.")
    private boolean isEitherOneRequired() {
        return (ccdCaseNumber == null && caseReference == null);
    }
}
