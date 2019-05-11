package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.payment.api.contract.FeeDto;

import java.math.BigDecimal;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "remissionDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class RemissionDto {

    private String id;

    private String remissionReference;

    private String hwfReference;

    private BigDecimal hwfAmount;

    private String beneficiaryName;

    private String ccdCaseNumber;

    private String caseReference;

    private String paymentGroupReference;

    private String paymentReference;

    private String feeCode;

    private FeeDto fee;
}
