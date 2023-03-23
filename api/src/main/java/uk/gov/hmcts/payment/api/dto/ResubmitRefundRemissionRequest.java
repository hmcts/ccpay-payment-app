package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@With
@JsonInclude(NON_NULL)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderMethodName = "resubmitRefundRemissionRequestWith")
public class ResubmitRefundRemissionRequest {

    @NotNull
    private String refundReason;

    @NotNull
    @Positive
    @Digits(integer = 10, fraction = 2, message = "Payment amount cannot have more than 2 decimal places")
    private BigDecimal amount;

    private String feeId;

    @NotNull
    @Positive
    @Digits(integer = 10, fraction = 2, message = "Total Refunded amount cannot have more than 2 decimal places")
    private BigDecimal totalRefundedAmount;
}
