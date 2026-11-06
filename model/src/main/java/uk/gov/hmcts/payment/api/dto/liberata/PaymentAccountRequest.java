package uk.gov.hmcts.payment.api.dto.liberata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonInclude(NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder(builderMethodName = "paymentAccountRequestWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class PaymentAccountRequest {

    @JsonProperty("PaymentByAccountRequest")
    private PaymentByAccountRequest  PaymentByAccountRequest;
}
