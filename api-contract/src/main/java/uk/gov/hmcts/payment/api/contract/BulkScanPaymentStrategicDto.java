package uk.gov.hmcts.payment.api.contract;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Builder(builderMethodName = "bulkScanPaymentStrategicDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class BulkScanPaymentStrategicDto {
    private String status;

    private String reference;

    private String paymentGroupReference;

    private List<PaymentAllocationDto> paymentAllocation;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "GMT")
    private Date dateCreated;

    private String ccdCaseNumber;

    private String caseReference;
}
