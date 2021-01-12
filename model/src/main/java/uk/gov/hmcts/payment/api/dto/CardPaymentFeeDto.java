package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.math.BigDecimal;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "cardPaymentFeeDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CardPaymentFeeDto {
    private Integer id;

    private BigDecimal calculatedAmount;

    private String code;

    private BigDecimal netAmount;

    private String version;

    private Integer volume;

    private String ccdCaseNumber;

    private String reference;

}
