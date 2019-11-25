package uk.gov.hmcts.payment.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Data
@Builder(builderMethodName = "feeWith")
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "fee")
public class PaymentFee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code")
    private String code;

    @Column(name = "version")
    private String version;

    @Column(name = "volume")
    private Integer volume;

    @Column(name = "calculated_amount")
    private BigDecimal calculatedAmount;

    @Column(name = "fee_amount")
    private BigDecimal feeAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_link_id", insertable = false, updatable = false)
    private PaymentFeeLink paymentLink;

    @Column(name = "ccd_case_number")
    private String ccdCaseNumber;

    @Column(name = "reference")
    private String reference;

    @Column(name = "net_amount")
    private BigDecimal netAmount;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "fee_id", referencedColumnName = "id")
    private List<Remission> remissions;
}
