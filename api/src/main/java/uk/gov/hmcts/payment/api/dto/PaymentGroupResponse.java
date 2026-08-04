package uk.gov.hmcts.payment.api.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Builder(builderMethodName = "paymentGroupResponseWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PaymentGroupResponse {

    private List<PaymentGroupDto> paymentGroups;
}
