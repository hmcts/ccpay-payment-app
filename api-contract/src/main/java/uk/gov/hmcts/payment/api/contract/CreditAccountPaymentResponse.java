package uk.gov.hmcts.payment.api.contract;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "creditAccountPaymentResponseWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreditAccountPaymentResponse {

    private String reference;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "GMT")
    private Date dateCreated;

    @NotEmpty
    private BigDecimal amount;

    private CurrencyCode currency;

    private String caseReference;

    private String ccdCaseNumber;

    private String status;

    private String serviceName;

    @NotEmpty
    private String siteId;

    @NotEmpty
    private String description;

    private String channel;

    private String method;

    private String externalReference;

    private String customerReference;

    private String organisationName;

    private String accountNumber;

    @NotNull
    private List<CreditAccountFeeDto> fees;

    @JsonProperty("_links")
    private PaymentDto.LinksDto links;

}
