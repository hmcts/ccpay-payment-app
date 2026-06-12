package uk.gov.hmcts.payment.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;


@Data
@AllArgsConstructor
@Builder(builderMethodName = "paymentByAccountRequestWith")
public class PaymentByAccountRequest {
    private String pbaNumber;
    private PaymentByAccountPayment payment;
}
