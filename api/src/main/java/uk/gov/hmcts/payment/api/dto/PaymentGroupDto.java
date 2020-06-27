package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.contract.PaymentDto;

import javax.validation.Valid;
import java.util.Date;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "paymentGroupDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PaymentGroupDto {

    private String paymentGroupReference;

    private Date dateCreated;

    private Date dateUpdated;

    private List<PaymentDto> payments;

    private List<RemissionDto> remissions;

    @Valid
    private List<FeeDto> fees;

}
