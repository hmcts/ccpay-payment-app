package uk.gov.hmcts.payment.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;

@Entity
@Data
@Builder(builderMethodName = "feePayStagedWith")
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "fee_pay_staged")
public class FeePayStaged {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ccd_case_no")
    private String ccdCaseNo;

    @Column(name = "group_reference")
    private String groupReference;

    @Column(name = "fee_id")
    private Integer feeId;

    @Column(name = "fee_code")
    private String feeCode;

    @Column(name = "fee_amount")
    private BigDecimal feeAmount;

    @Column(name = "volume")
    private Integer volume;

    @Column(name = "calculated_amount")
    private BigDecimal calculatedAmount;

    @Column(name = "hwf_amount")
    private BigDecimal hwfAmount;

    @Column(name = "net_amount")
    private BigDecimal netAmount;

    @CreationTimestamp
    @Column(name = "fee_created_date", nullable = false)
    private Timestamp feeCreatedDate;

    @Column(name = "payment_id")
    private Integer paymentId;

    @Column(name = "payment_amount")
    private BigDecimal paymentAmount;

    @Column(name = "payment_ref")
    private String paymentRef;

    @Column(name = "payment_status")
    private String paymentStatus;

    @CreationTimestamp
    @Column(name = "payment_created_date", nullable = false)
    private Date paymentCreatedDate;

    @Column(name = "payment_channel")
    private String paymentChannel;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "payment_provider")
    private String paymentProvider;

    @Column(name = "service_type")
    private String serviceType;
}
