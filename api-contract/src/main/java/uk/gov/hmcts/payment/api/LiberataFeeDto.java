package uk.gov.hmcts.payment.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@SuperBuilder(builderMethodName = "buildLiberataFeeDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class LiberataFeeDto extends EnrichablePaymentFeeDto{
    private String caseReference;

    private BigDecimal apportionedPayment;

    private Date dateReceiptProcessed;

    private String paymentGroupReference;
}
