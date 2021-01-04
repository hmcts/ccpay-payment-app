package uk.gov.hmcts.payment.api.contract;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import javax.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "retrievePaymentResponseWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreditAccountPaymentStatusResponse {
    private String reference;

    @NotEmpty
    private BigDecimal amount;

    private String status;

    private List<StatusHistoryDto> statusHistories;
}
