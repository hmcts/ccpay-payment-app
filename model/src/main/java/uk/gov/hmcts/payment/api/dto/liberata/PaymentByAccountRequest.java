package uk.gov.hmcts.payment.api.dto.liberata;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder(builderMethodName = "paymentByAccountRequestWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PaymentByAccountRequest {
    private String pbaNumber;
    private PaymentRequest payment;
}
