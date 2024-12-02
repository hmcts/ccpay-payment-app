package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;

import jakarta.validation.Valid;
import java.util.Date;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "paymentGroupDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PaymentGroupDto {

    private String paymentGroupReference;

    private Date dateCreated;

    private Date dateUpdated;

    private List<PaymentDto> payments;

    private List<RemissionDto> remissions;

    @JsonProperty("service_request_status")
    private String serviceRequestStatus;

    @Valid
    private List<FeeDto> fees;

    private boolean isAnyPaymentDisputed;

    private List<RefundDto> refunds;

}
