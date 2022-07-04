package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "paymentFailureResponseWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PaymentFailureResponseDto {
     private PaymentFailureInitiatedDto paymentFailureInitiated;
     private PaymentFailureClosedDto paymentFailureClosed;

}
