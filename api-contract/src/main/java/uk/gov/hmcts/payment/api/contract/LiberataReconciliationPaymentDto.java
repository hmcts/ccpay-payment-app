package uk.gov.hmcts.payment.api.contract;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import lombok.experimental.SuperBuilder;
import uk.gov.hmcts.payment.api.LiberataFeeDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@SuperBuilder(builderMethodName = "liberataPaymentReconciliationResponse")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LiberataReconciliationPaymentDto extends PaymentResponse{

    private Date bankedDate;

    private List<PaymentAllocationDto> paymentAllocation;

    @NotNull
    private List<LiberataFeeDto> fees;

}
