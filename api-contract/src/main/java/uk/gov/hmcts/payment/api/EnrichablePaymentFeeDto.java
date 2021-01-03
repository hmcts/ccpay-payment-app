package uk.gov.hmcts.payment.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import uk.gov.hmcts.payment.api.contract.PaymentFeeDto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@SuperBuilder(builderMethodName = "buildEnrichablePaymentFeeDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class EnrichablePaymentFeeDto extends PaymentFeeDto {
    @NotEmpty
    private String code;

    private String jurisdiction1;

    private String jurisdiction2;

    private BigDecimal allocatedAmount;

    private BigDecimal apportionAmount;

    private Date dateCreated;

    private Date dateUpdated;

    private Date dateApportioned;

    @NotEmpty
    private String version;

    private String memoLine;

    private String naturalAccountCode;

}
