package uk.gov.hmcts.payment.api.contract;

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
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderMethodName = "createCreditAccountPaymentRequestFeeWith")
public class CreditAccountPaymentRequestFee {
//    private BigDecimal allocatedAmount;
//
//    private BigDecimal amountDue;
//
//    private BigDecimal apportionAmount;
//
//    private BigDecimal apportionedPayment;

    @NotNull
    @Digits(integer = 10, fraction = 2, message = "Fee calculated amount cannot have more than 2 decimal places")
    private BigDecimal calculatedAmount;

//    private String caseReference;
//
//    private String ccdCaseNumber;

    @NotEmpty
    private String code;

//    private Date dateApportioned;
//
//    private Date dateCreated;
//
//    private Date dateReceiptProcessed;
//
//    private Date dateUpdated;
//
//    private String description;
//
//    private BigDecimal feeAmount;
//
//    private Integer id;
//
//    private String jurisdiction1;
//
//    private String jurisdiction2;
//
//    private String memoLine;
//
//    private String naturalAccountCode;
//
//    @Digits(integer = 10, fraction = 2, message = "Net amount cannot have more than 2 decimal places")
//    private BigDecimal netAmount;
//
//    private String paymentGroupReference;
//
//    private String reference;

    @NotEmpty
    private String version;

    @Positive
    private Integer volume;

}
