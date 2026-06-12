package uk.gov.hmcts.payment.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder(builderMethodName = "paymentByAccountPaymentWith")
public class PaymentByAccountPayment {
    private String serviceName;
    private String groupReference;
    private String paymentReference;
    private String dateCreated;
    private String amount;
    private String currency;
    private String siteId;
    private String caseReference;
    private String ccdCaseNumber;
    private String customerReference;
    private String surname;
    private  PaymentByAccountFee paymentByAccountFee;
}
