package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;
import org.joda.time.DateTime;
import org.springframework.format.annotation.DateTimeFormat;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.IllegalFormatException;
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

    @AssertFalse(message = "Invalid payment reported offline date.")
    private boolean isValidReportedDateOffline() {
        if (reportedDateOffline != null) {
            try {
                DateTime.parse(reportedDateOffline);
            } catch (IllegalArgumentException fe) {
                return true;
            }
        }

        return false;
    }
}
