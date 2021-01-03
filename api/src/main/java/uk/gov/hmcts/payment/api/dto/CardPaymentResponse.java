package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.LinksDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.PaymentFeeDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "cardPaymentWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CardPaymentResponse {
    private String reference;

    @NotEmpty
    private BigDecimal amount;

    private CurrencyCode currency;

    private String ccdCaseNumber;

    private String caseReference;

    private String status;

    @NotEmpty
    private String siteId;

    private String serviceName;

    @NotEmpty
    private String description;

    private String channel;

    private String method;

    private String externalReference;

    private String paymentGroupReference;

    private String externalProvider;

    @NotNull
    private List<PaymentFeeDto> fees;

    @JsonProperty("_links")
    private LinksDto links;

}
