package uk.gov.hmcts.payment.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Data
@Builder(builderMethodName = "remissionWith")
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "remission", indexes = {
    @Index(name = "ix_remission_hwf_reference", columnList = "hwf_reference"),
    @Index(name = "ix_remission_ccd_case_number", columnList = "ccd_case_number"),
    @Index(name = "ix_remission_remission_reference", columnList = "remission_reference")
})
public class Remission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "remission_reference")
    private String remissionReference;

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

    @Column(name = "site_id")
    private String siteId;

    @CreationTimestamp
    @Column(name = "date_created")
    private Date dateCreated;

    @UpdateTimestamp
    @Column(name = "date_updated")
    private Date dateUpdated;

    @Column(name = "retrospective_reason")
    private String retrospectiveReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_link_id", insertable = false, updatable = false)
    private PaymentFeeLink paymentFeeLink;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_id", insertable = false, updatable = false)
    private PaymentFee fee;

}
