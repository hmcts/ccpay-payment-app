package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.bind.DefaultValue;
import uk.gov.hmcts.payment.api.contract.RefundsFeeDto;
import uk.gov.hmcts.payment.api.model.ContactDetails;

import javax.validation.Valid;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderMethodName = "refundRequestWith")
public class PaymentRefundRequest {

    @NotNull(message = "Payment Reference cannot be null")
    @NotEmpty(message = "Payment Reference cannot be blank")
    private String paymentReference;

    @NotNull(message = "Refund Reason cannot be null")
    @NotEmpty(message = "Refund Reason cannot be blank")
    private String refundReason;

    @Digits(integer = 10, fraction = 2, message = "Please check the amount you want to refund")
    @NotNull(message = "You need to enter a refund amount")
    private BigDecimal totalRefundAmount;

    @NotEmpty
    @Valid
    private List<RefundsFeeDto>  fees;

    @NotNull(message = "Contact Details cannot be null")
    private ContactDetails contactDetails;

    private boolean isOverPayment;
}
