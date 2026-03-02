package uk.gov.hmcts.payment.api.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "reconciliationFeeDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReconciliationFeeDto {

    private Integer id;
    private String code;
    private String version;
    private Integer volume;
    private BigDecimal calculatedAmount;
    private String memoLine;
    private String naturalAccountCode;
    private String ccdCaseNumber;
    private String caseReference;
    private String reference;
    private String jurisdiction1;
    private String jurisdiction2;
    private String paymentGroupReference;
    private BigDecimal apportionedPayment;
    private Date dateReceiptProcessed;
}
