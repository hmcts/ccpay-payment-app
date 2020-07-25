package uk.gov.hmcts.payment.api.model;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import uk.gov.hmcts.payment.api.jpaaudit.listner.Auditable;
import uk.gov.hmcts.payment.api.jpaaudit.listner.FeePayApportionEntityListener;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@EntityListeners(FeePayApportionEntityListener.class)
@Data
@Builder(builderMethodName = "feePayApportionWith")
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "fee_pay_apportion")
public class FeePayApportion extends Auditable<String> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "payment_id")
    private Integer paymentId;

    @Column(name = "fee_id")
    private Integer feeId;

    @Column(name = "fee_amount")
    private BigDecimal feeAmount;

    @Column(name = "payment_amount")
    private BigDecimal paymentAmount;

    @Column(name = "apportion_amount")
    private BigDecimal apportionAmount;

    @Column(name = "allocated_amount")
    private BigDecimal allocatedAmount;

    @Column(name = "apportion_type")
    private String apportionType;

    @ToString.Exclude
    @Column(name = "is_fully_apportioned")
    private String isFullyApportioned;

    @ToString.Exclude
    @Column(name = "curr_apportion_amount")
    private BigDecimal currApportionAmount;

    @Column(name = "call_short_fall_amount")
    private BigDecimal callShortFallAmount;

    @Column(name = "call_surplus_amount")
    private BigDecimal callSurplusAmount;

    @ToString.Exclude
    @Column(name = "ccd_case_number")
    private String ccdCaseNumber;

    @ToString.Exclude
    @Column(name = "created_by")
    private String createdBy;

    @CreationTimestamp
    @Column(name = "date_created", nullable = false)
    private Date dateCreated;
}
