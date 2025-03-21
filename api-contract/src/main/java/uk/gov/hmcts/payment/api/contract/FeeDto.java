package uk.gov.hmcts.payment.api.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "feeDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class FeeDto {

    private Integer id;

    @NotEmpty
    private String code;

    @NotEmpty
    private String version;

    @Positive
    private Integer volume;

    @NotNull
    @Digits(integer = 10, fraction = 2, message = "Fee calculated amount cannot have more than 2 decimal places")
    private BigDecimal calculatedAmount;

    private BigDecimal feeAmount;

    private String memoLine;

    private String naturalAccountCode;

    private String ccdCaseNumber;

    private String reference;

    @Digits(integer = 10, fraction = 2, message = "Net amount cannot have more than 2 decimal places")
    private BigDecimal netAmount;

    private String jurisdiction1;

    private String jurisdiction2;

    private String description;

    private String caseReference;

    private BigDecimal apportionAmount;

    private BigDecimal allocatedAmount;

    private Date dateApportioned;

    private Date dateCreated;

    private Date dateUpdated;

    private BigDecimal amountDue;

    // The below 3 fields added as part of apportionment changes for Liberata

    private String paymentGroupReference;

    private BigDecimal apportionedPayment;

    private Date dateReceiptProcessed;

    private Boolean remissionEnable;

    private boolean issueRefundAddRefundAddRemission;

    private BigDecimal overPayment;

    private boolean addRemission;


    @Override
    public String toString() {
        return
            "Fee DTO -  id='" + id + '\'' +
                "code ='" + code + '\'' +
                "apportionedPayment ='" + apportionedPayment + '\'' +
                "naturalAccountCode ='" + naturalAccountCode + '\'' +
                "volume ='" + volume + '\'' +
                "calculatedAmount ='" + calculatedAmount + '\''
                ;
    }
}
