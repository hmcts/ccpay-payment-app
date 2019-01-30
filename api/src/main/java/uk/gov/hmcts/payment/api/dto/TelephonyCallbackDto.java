package uk.gov.hmcts.payment.api.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TelephonyCallbackDto {

    private String orderCurrency;
    private String orderAmount;
    private String orderReference;
    private String ppAccountID;
    private String transactionResult;
    private String transactionAuthCode;
    private String transactionID;
    private String transactionResponseMsg;
    @ToString.Exclude
    private String cardExpiry;
    private String cardLast4;
    private String ppCallID;
    private String customData1;
    private String customData2;
    private String customData3;
    private String customData4;

}
