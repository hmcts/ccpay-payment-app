package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.payment.api.model.ContactDetails;

import java.math.BigDecimal;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Builder(builderMethodName = "buildRefundListDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
public class RefundDto {

    private String ccdCaseNumber;

    private BigDecimal amount;

    private String reason;

    private RefundStatus refundStatus;

    private String refundReference;

    private String paymentReference;

    private String userFullName;

    private String emailId;

    private String dateCreated;

    private String dateUpdated;

    private ContactDetails contactDetails;

}
