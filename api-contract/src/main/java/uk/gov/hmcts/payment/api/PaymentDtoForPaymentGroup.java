package uk.gov.hmcts.payment.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import uk.gov.hmcts.payment.api.contract.PaymentAllocationDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;

import javax.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "paymentDtoForPaymentGroupWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PaymentDtoForPaymentGroup {
    private String reference;

    @NotEmpty
    private String description;

    private String serviceName;

    @NotEmpty
    private String siteId;

    @NotEmpty
    private BigDecimal amount;

    private String caseReference;

    private String ccdCaseNumber;

    private String accountNumber;

    private String organisationName;

    private String customerReference;

    private String channel;

    private CurrencyCode currency;

    private String status;

    private List<PaymentAllocationDto> paymentAllocation;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "GMT")
    private Date dateCreated;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "GMT")
    private Date dateUpdated;

    private String method;

    private Date bankedDate;

    private String externalProvider;

    private String payerName;

    private String documentControlNumber;

    private String externalReference;

}
