package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
    @DecimalMin("0.01")
    @Positive
    @Digits(integer = 10, fraction = 2, message = "Payment amount cannot have more than 2 decimal places")
    private BigDecimal amount;

    @NotNull
    private PaymentMethodType paymentMethod;

    @NotEmpty
    @JsonProperty("requestor_reference")
    private String reference;

    @NotNull
    @JsonProperty("requestor")
    private Service service;

    private CurrencyCode currency;

    private String externalReference;

    private String externalProvider;

    private String giroSlipNo;

    @NotNull
    private String reportedDateOffline;

    @NotEmpty
    @JsonProperty("site_id")
    private String siteId;

    @NotEmpty
    @Valid
    private List<FeeDto> fees;

    @JsonIgnore
    @AssertFalse(message = "Invalid payment reported offline date. Date format should be ISO.")
    public boolean isValidReportedDateOffline() {
        if (reportedDateOffline != null) {
            try {
                LocalDate.parse(reportedDateOffline, DateTimeFormatter.ISO_DATE);
            } catch (DateTimeParseException e) {
                return true;
            }
        }

        return false;
    }
}
