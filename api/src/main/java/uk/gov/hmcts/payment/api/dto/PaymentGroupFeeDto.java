package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "paymentGroupFeeDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PaymentGroupFeeDto {
    @NotNull
    @Digits(integer = 10, fraction = 2, message = "Fee calculated amount cannot have more than 2 decimal places")
    private BigDecimal calculatedAmount;

    @NotEmpty
    private String code;

    @Digits(integer = 10, fraction = 2, message = "Net amount cannot have more than 2 decimal places")
    private BigDecimal netAmount;

    @NotEmpty
    private String version;

    @Positive
    private Integer volume;

    private BigDecimal feeAmount;

    private String ccdCaseNumber;

    private String reference;

    private Integer id;

    private String memoLine;

    private String naturalAccountCode;

    private String description;

    private BigDecimal allocatedAmount;

    private BigDecimal apportionAmount;

    private Date dateCreated;

    private Date dateUpdated;

    private Date dateApportioned;

    private BigDecimal amountDue;

}
