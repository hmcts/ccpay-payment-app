package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import uk.gov.hmcts.payment.api.contract.StatusHistoryDto;

import java.math.BigDecimal;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "cardPaymentStatusWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RetrieveCardPaymentStatusResponse {

    private String reference;

    private BigDecimal amount;

    private String paymentGroupReference;

    private String status;

    private List<StatusHistoryDto> statusHistories;

}
