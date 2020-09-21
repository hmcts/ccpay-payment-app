package uk.gov.hmcts.payment.api.external.client.dto;

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
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderMethodName = "pciPALAntennaRequestWith")
public class PCIPALRequest {

    private String FlowId;
    private InitialValues InitialValues;

    @Data
    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
    @JsonInclude(NON_NULL)
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder(builderMethodName = "initialValuesWith")
    public static class InitialValues {
        private String orderId;
        private String amount;
        private String currencyCode;
        private String callbackURL;
        private String returnURL;
    }
}
