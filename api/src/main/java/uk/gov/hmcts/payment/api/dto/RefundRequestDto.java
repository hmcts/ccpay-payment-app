package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.payment.api.model.ContactDetails;
import uk.gov.hmcts.payment.api.model.PaymentChannel;
import uk.gov.hmcts.payment.api.model.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderMethodName = "refundRequestDtoWith")
public class RefundRequestDto {

    private String paymentReference;

    private String refundReason;

    private BigDecimal refundAmount;

    private BigDecimal paymentAmount;

    private String ccdCaseNumber;

    private String feeIds;

    private List<RefundFeesDto> refundFees;

    private String serviceType;

    private String paymentMethod;

    private ContactDetails contactDetails;

    private PaymentChannel paymentChannel;

}
