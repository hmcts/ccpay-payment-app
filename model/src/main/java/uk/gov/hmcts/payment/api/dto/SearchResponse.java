package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "searchResponseWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SearchResponse implements Serializable {
    private String ccdReference;
    private String exceptionRecordReference;
    private String responsibleServiceId;
//    private List<PaymentMetadataDto> payments;
//    private PaymentStatus allPaymentsStatus;
}
