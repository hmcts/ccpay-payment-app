package uk.gov.hmcts.payment.api.contract;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.payment.api.dto.LiberataSupplementaryInfo;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PaymentsResponse {

    private List<PaymentDto> payments;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("supplementary_info")
    private List<LiberataSupplementaryInfo> supplementaryInfo;

    // Constructor for just payments
    public PaymentsResponse(List<PaymentDto> payments) {
        this.payments = payments;
    }

}
