package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderMethodName = "paymentStatusUpdateSecondWith")
public class PaymentStatusUpdateSecond {

    @NotNull(message = "Representment Status cannot be null")
    @NotEmpty(message = "Representment Status cannot be blank")
    private String representmentStatus;

    @NotNull(message = "Representment Date cannot be null")
    @NotEmpty(message = "Representment Date cannot be blank")
    private String representmentDate;
}
