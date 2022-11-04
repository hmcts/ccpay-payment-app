package uk.gov.hmcts.payment.api.contract;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Builder(builderMethodName = "buildDisputeDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
public class DisputeDto {

    private int pingNumber;
    private boolean isDispute;
    private String failureReference;
    private String reason;
    private String paymentReference;
    private String ccdCaseNumber;
    private BigDecimal amount;
    private String dcn;
    private Date failureEventDateTime;
    private String hasAmountDebited;
    private String representmentSuccess;
    private Date representmentOutcomeDate;
    private String failureType;
}
