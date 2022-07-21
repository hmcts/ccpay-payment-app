package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "paymentFailureReportDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PaymentFailureReportDto {

   private String paymentReference;

   private String ccdReference;

   private String orgId;

    private String serviceName;

    private String failureReference;

    private String failureReason;

    private BigDecimal disputedAmount;

    private String eventName;

    private Date eventDate;

    private String representmentStatus;

    private Date representmentDate;

    private String refundReference;

    private String refundAmount;

    private String RefundDate;

}
