package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "pciPalPaymentRequestWith")
@AllArgsConstructor
@NoArgsConstructor
public class PciPalPaymentRequest {

    private String orderCurrency;
    private String orderAmount;
    private String orderReference;
    private String ppAccountID;
    private String apiKey;
    private String renderMethod;
    private String callbackURL;
    private String customData1;

}
