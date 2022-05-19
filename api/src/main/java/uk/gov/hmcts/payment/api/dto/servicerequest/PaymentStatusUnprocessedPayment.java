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
@Builder(builderMethodName = "paymentStatusUnprocessedWith")
public class PaymentStatusUnprocessedPayment {

    private String failureReference;

    private String reason;

    private String poBoxNumber;
    private BigDecimal amount;

    private String dcn;
    private String event_date_time;
}
