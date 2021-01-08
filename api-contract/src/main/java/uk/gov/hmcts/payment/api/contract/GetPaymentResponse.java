package uk.gov.hmcts.payment.api.contract;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import uk.gov.hmcts.payment.api.EnrichablePaymentDto;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@SuperBuilder(builderMethodName = "getPaymentResponseWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class GetPaymentResponse  extends EnrichablePaymentDto {
    private List<StatusHistoryDto> statusHistories;

    private List<PaymentAllocationDto> paymentAllocation;

    private Date bankedDate;

    private String payerName;

    private String documentControlNumber;

}
