package uk.gov.hmcts.payment.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.payment.api.model.PaymentFee;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@Builder(builderMethodName = "retroRemissionServiceRequestWith")
public class RetroRemissionServiceRequest {
    private String hwfReference;
    private BigDecimal hwfAmount;
}
