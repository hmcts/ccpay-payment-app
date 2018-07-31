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
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.contract.util.Method;
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
@Builder(builderMethodName = "createPaymentRecordRequestDtoWith")
@Wither
public class PaymentRecordRequest {

    @NotNull
    @Min(1)
    @Digits(integer = 10, fraction = 2, message = "Payment amount cannot have more than 2 decimal places")
    private BigDecimal amount;

    @NotNull
    private Method paymentMethod;

    @NotEmpty
    private String reference;

    @NotNull
    private Service service;

    private CurrencyCode currency;

    private String externalReference;

    private String externalProvider;

    private String giroSlipNo;

    @NotEmpty
    @JsonProperty("site_id")
    private String siteId;

    @NotEmpty
    @Valid
    private List<FeeDto> fees;

}
