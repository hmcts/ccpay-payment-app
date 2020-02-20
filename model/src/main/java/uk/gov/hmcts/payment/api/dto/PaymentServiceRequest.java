package uk.gov.hmcts.payment.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.payment.api.model.PaymentFee;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@Builder(builderMethodName = "paymentServiceRequestWith")
public class PaymentServiceRequest {
    private String paymentGroupReference;
    private String paymentReference;
    private String description;
    private String returnUrl;
    private String ccdCaseNumber;
    private String caseReference;
    private String currency;
    private String siteId;
    private String serviceType;
    private List<PaymentFee> fees;
    private BigDecimal amount;
    private String serviceCallbackUrl;
    private String channel;
    private String provider;
    private String language;
}
