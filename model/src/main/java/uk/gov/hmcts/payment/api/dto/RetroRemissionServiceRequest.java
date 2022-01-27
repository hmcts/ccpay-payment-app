package uk.gov.hmcts.payment.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@Builder(builderMethodName = "retroRemissionServiceRequestWith")
public class RetroRemissionServiceRequest {
    private String hwfReference;
    private BigDecimal hwfAmount;
}
