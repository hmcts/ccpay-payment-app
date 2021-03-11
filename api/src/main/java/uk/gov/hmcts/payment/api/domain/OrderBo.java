package uk.gov.hmcts.payment.api.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

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
    private String ccdCaseNo;
    private List<OrderFeeBo> fees;

    public void validate(){
        //--fee validation logic for duplicate Fees in Request
        //--CCD Case 16 digit check
    }

}
