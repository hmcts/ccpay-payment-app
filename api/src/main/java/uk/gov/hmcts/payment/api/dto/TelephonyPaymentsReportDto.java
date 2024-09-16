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
import uk.gov.hmcts.payment.api.model.PaymentStatus;

import java.math.BigDecimal;
import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "telephonyPaymentsReportDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TelephonyPaymentsReportDto {
    @JsonProperty("Resp_Service Name")
    private String serviceName;

    @JsonProperty("CCD_Ref")
    private String ccdReference;

    @JsonProperty("Payment Reference")
    private String paymentReference;

    @JsonProperty("Fee Code")
    private String feeCode;

    @JsonProperty("Payment Date")
    private Date paymentDate;

    @JsonProperty("Amount")
    private BigDecimal amount;

    @JsonProperty("Payment Status")
    private String paymentStatus;

}
