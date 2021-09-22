package uk.gov.hmcts.payment.api.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "serviceRequestOnlinePaymentBo")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Component
public class ServiceRequestOnlinePaymentBo {

    private String paymentReference;
    private String currency;
    private String serviceCallbackUrl;
    private String returnUrl;
    private String description;
    private String language;
    private String internalReference;
    private String userId;
    private String s2sServiceName;
    private BigDecimal amount;

}
