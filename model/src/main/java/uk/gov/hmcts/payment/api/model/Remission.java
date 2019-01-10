package uk.gov.hmcts.payment.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.math.BigDecimal;

@Entity
@Data
@Builder(builderMethodName = "remissionWith")
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "remissions", indexes = {
    @Index(name = "ix_remission_hwf_reference", columnList = "hwf_reference"),
    @Index(name = "ix_remission_ccd_case_number", columnList = "ccd_case_number")
})
public class Remission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "hwf_reference")
    private String hwfReference;

    @Column(name = "hwf_amount")
    private BigDecimal hwfAmount;

    @Column(name = "beneficiary_name")
    private String beneficiaryName;

    @Column(name = "ccd_case_number")
    private String ccdCaseNumber;

    @Column(name = "case_reference")
    private String caseReference;

    @Column(name = "payment_group_reference")
    private String paymentGroupReference;
}
