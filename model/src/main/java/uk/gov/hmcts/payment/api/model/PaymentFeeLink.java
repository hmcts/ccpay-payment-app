package uk.gov.hmcts.payment.api.model;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import uk.gov.hmcts.payment.api.jpaaudit.listner.Auditable;
import uk.gov.hmcts.payment.api.jpaaudit.listner.PaymentFeeLinkEntityListener;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@EntityListeners(PaymentFeeLinkEntityListener.class)
@Getter
@Setter
@ToString
@Builder(builderMethodName = "paymentFeeLinkWith")
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "payment_fee_link")
public class PaymentFeeLink{

    @ToString.Exclude
    @ManyToMany(cascade =CascadeType.ALL )
    @JoinTable(
        name = "order_cases",
        joinColumns = @JoinColumn(name = "order_id"),
        inverseJoinColumns = @JoinColumn(name = "case_details_id")
    )
    private Set<CaseDetails> caseDetails = new HashSet<>();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "payment_reference")
    private String paymentReference;

    @Column(name = "enterprise_service_name")
    private String enterpriseServiceName;

    @Column(name = "org_id")
    private String orgId;

    @CreationTimestamp
    @Column(name = "date_created", nullable = false)
    private Date dateCreated;

    @UpdateTimestamp
    @Column(name = "date_updated", nullable = false)
    private Date dateUpdated;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "payment_link_id", referencedColumnName = "id", nullable = false)
    @ToString.Exclude
    private List<Payment> payments;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "payment_link_id", referencedColumnName = "id", nullable = false)
    @ToString.Exclude
    private List<PaymentFee> fees;


    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "payment_link_id", referencedColumnName = "id", nullable = false)
    @ToString.Exclude
    private List<Remission> remissions;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "payment_link_id", referencedColumnName = "id", nullable = false)
    @ToString.Exclude
    private List<FeePayApportion> apportions;

    //Need to remove after all services onboarded order integration
    @ToString.Exclude
    @Column(name = "ccd_case_number")
    @Transient
    private String ccdCaseNumber;

    @Override
    public int hashCode() {
        return super.hashCode();
    }

}
