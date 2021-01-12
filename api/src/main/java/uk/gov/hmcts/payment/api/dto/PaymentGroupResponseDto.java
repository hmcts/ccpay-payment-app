package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import uk.gov.hmcts.payment.api.PaymentDtoForPaymentGroup;

import java.util.Date;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "paymentGroupResponseDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PaymentGroupResponseDto {
    private String paymentGroupReference;

    private Date dateCreated;

    private Date dateUpdated;

    private List<PaymentDtoForPaymentGroup> payments;

    private List<RemissionDto> remissions;

    private List<PaymentGroupFeeDto> fees;
}
