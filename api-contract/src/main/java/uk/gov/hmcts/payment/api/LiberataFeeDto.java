package uk.gov.hmcts.payment.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@SuperBuilder(builderMethodName = "buildLiberataFeeDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LiberataFeeDto extends EnrichablePaymentFeeDto{
    private String caseReference;

    private String paymentGroupReference;
}
