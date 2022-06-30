package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "paymentFailureResponseDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PaymentFailureDto {

    private String Status;
    private String paymentReference;
    private String failureReason;
    private BigDecimal disputedAmount;
    private String additionalReference;
    private Date failureEventDateTime;
    private String hasAmountDebited;
    private Date representmentOutcomeDate;
    private String failureType;
    private String failureReference;
    private String representmentStatus;
}
