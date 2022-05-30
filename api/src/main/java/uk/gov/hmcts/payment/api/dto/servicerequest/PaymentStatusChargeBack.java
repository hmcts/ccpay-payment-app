package uk.gov.hmcts.payment.api.dto.servicerequest;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderMethodName = "paymentStatusChargeBackRequestWith")
public class PaymentStatusChargeBack {

    private String paymentReference;

    private String failureReference;

    private String ccdCaseNumber;

    private String reason;

    private BigDecimal amount;

    private String additionalReference;
    private String event_date_time;
    private String has_amount_debited;
}
