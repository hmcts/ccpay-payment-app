package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import lombok.*;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import java.util.List;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "supplementaryPaymentDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SupplementaryPaymentDto {
    private List<PaymentDto> payments;

    @JsonProperty("supplementary_info")
    private List<LiberataSupplementaryInfo> supplementaryInfo;
 }
