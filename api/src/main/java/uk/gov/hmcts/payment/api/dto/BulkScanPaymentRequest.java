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
import org.joda.time.DateTime;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.contract.util.Service;
import uk.gov.hmcts.payment.api.model.PaymentChannel;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;

import javax.validation.constraints.*;
import java.math.BigDecimal;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderMethodName = "createBulkScanPaymentWith")
@Wither
public class BulkScanPaymentRequest {

    @NotNull
    @DecimalMin("0.01")
    @Positive
    @Digits(integer = 10, fraction = 2, message = "Payment amount cannot have more than 2 decimal places")
    private BigDecimal amount;

    @NotNull
    private PaymentMethodType paymentMethod;

    @NotNull
    @JsonProperty("requestor")
    private Service service;

    private String ccdCaseNumber;

    private PaymentChannel paymentChannel;

    private CurrencyCode currency;

    private String externalProvider;

    private String giroSlipNo;

    @NotNull
    private String bankedDate;

    @NotEmpty
    @JsonProperty("site_id")
    private String siteId;

    private String payerName;

    @NotEmpty
    @NotNull
    private String documentControlNumber;

    @JsonIgnore
    @AssertFalse(message = "Invalid payment banked date. Date format should be UTC.")
    public boolean isValidBankedDate() {
        if (bankedDate != null) {
            try {
                DateTime.parse(bankedDate);
            } catch (IllegalArgumentException fe) {
                return true;
            }
        }
        return false;
    }
}
