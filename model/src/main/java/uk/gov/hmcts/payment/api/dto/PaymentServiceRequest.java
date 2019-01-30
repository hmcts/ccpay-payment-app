package uk.gov.hmcts.payment.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.payment.api.model.PaymentFee;

import java.util.List;

@Data
@AllArgsConstructor
@Builder(builderMethodName = "paymentServiceRequestWith")
public class PaymentServiceRequest {
    String paymentReference;
    String description;
    String returnUrl;
    String ccdCaseNumber;
    String caseReference;
    String currency;
    String siteId;
    String serviceType;
    List<PaymentFee> fees;
    int amount;
    String serviceCallbackUrl;
    String channel;
    String provider;
}
