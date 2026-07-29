package uk.gov.hmcts.payment.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder(builderMethodName = "paymentByAccountFeeWith")
public class PaymentByAccountFee {
    private String code;
    private Integer id;
    private String version;
    private String memosline;
    private String nac;
    private String jurisdiction1;
    private String jurisdiction2;
    private String volume;
    private String calculatedAmount;

}
