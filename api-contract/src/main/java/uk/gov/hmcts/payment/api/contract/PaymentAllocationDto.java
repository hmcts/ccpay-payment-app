package uk.gov.hmcts.payment.api.contract;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.payment.api.model.PaymentAllocationStatus;

import javax.validation.constraints.NotNull;
import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "paymentAllocationDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PaymentAllocationDto {


    private String id;

    @NotNull
    private String paymentReference;

    @NotNull
    private String paymentGroupReference;

    @NotNull
    private PaymentAllocationStatus paymentAllocationStatus;

    private String unidentifiedReason;

    private String receivingOffice;

    private String receivingEmailAddress;

    private String sendingEmailAddress;

    private String userId;

    // This field added due to Libereta Changes. This is just a duplication of paymentAllocationStatus and unidentifiedReason parameters.
    private String allocationStatus;

    private String allocationReason;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "GMT")
    private Date dateCreated;

}
