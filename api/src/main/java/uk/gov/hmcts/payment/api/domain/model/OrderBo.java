package uk.gov.hmcts.payment.api.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import uk.gov.hmcts.payment.api.model.PaymentStatus;

import java.math.BigDecimal;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "orderBoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrderBo {

    private String reference;

    private String ccdCaseNumber;

    private List<OrderFeeBo> fees;

    private PaymentStatus status;

    private BigDecimal orderBalance;

    //-- All CRUD & Validation operations for Orders to be implemented

    public void validate(){
        //--fee validation logic for duplicate Fees in Request
        //--CCD Case 16 digit check
    }

    public void canAcceptPayment(){
        // Validate if Order has outstanding Balance
        // false : reject Payment
        // true : Continue
    }

    public void getStatus() {

    }


}
