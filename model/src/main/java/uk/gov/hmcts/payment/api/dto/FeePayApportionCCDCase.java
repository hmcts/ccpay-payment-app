package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.Remission;

import java.util.LinkedHashSet;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderMethodName = "feePayApportionCCDCaseWith")
@Data
public class FeePayApportionCCDCase {

    private String ccdCaseNo;

    private LinkedHashSet<PaymentFeeLink> feePayGroups;

    private LinkedHashSet<PaymentFee> fees;

    private LinkedHashSet<Remission> remissions;

    private LinkedHashSet<Payment> payments;
}
