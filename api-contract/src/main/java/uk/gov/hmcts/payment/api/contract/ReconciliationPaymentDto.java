package uk.gov.hmcts.payment.api.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "reconciliationPaymentDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReconciliationPaymentDto {

    private String paymentReference;
    private String paymentGroupReference;
    private String serviceName;
    private String siteId;
    private BigDecimal amount;
    private String caseReference;
    private String ccdCaseNumber;
    private String accountNumber;
    private String organisationName;
    private String customerReference;
    private String channel;
    private CurrencyCode currency;
    private String status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "GMT")
    private Date dateCreated;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "GMT")
    private Date dateUpdated;
    private String method;
    private String giroSlipNo;
    private String externalProvider;
    private String externalReference;
    private Date reportedDateOffline;
    private List<ReconciliationFeeDto> fees;
    private List<ReconciliationPaymentAllocationDto> paymentAllocation;
}
