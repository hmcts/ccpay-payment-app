package uk.gov.hmcts.payment.api.contract;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.payment.api.model.PaymentAllocationStatus;

import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "reconciliationPaymentAllocationDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReconciliationPaymentAllocationDto {

    private String id;
    private String paymentReference;
    private String paymentGroupReference;
    private PaymentAllocationStatus paymentAllocationStatus;
    private String unidentifiedReason;
    private String receivingOffice;
    private String reason;
    private String explanation;
    private String userId;
    private String userName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "GMT")
    private Date dateCreated;
}
