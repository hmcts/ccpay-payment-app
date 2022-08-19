package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@SuperBuilder(builderMethodName = "paymentFailureClosedResponseDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PaymentFailureClosedDto extends PaymentFailureInitiatedDto{

    private String representmentStatus;
    private Date representmentDate;
}
