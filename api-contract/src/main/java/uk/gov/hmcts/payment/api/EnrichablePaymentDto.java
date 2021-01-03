package uk.gov.hmcts.payment.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import uk.gov.hmcts.payment.api.contract.PaymentResponse;

import javax.validation.constraints.NotNull;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@SuperBuilder(builderMethodName = "enrichablePaymentDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class EnrichablePaymentDto extends PaymentResponse {
    @NotNull
    private List<EnrichablePaymentFeeDto> fees;
}
