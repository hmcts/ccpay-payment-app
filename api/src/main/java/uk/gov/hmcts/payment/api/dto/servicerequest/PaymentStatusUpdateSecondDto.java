package uk.gov.hmcts.payment.api.dto.servicerequest;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderMethodName = "paymentStatusUpdateRequestWith")
public class PaymentStatusUpdateSecondDto {

    private String representmentSuccess;

    private String representmentOutcomeDate;
}
