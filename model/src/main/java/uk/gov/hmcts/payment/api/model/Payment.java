package uk.gov.hmcts.payment.api.model;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import javax.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Data
@Builder(builderMethodName = "paymentWith")
@AllArgsConstructor
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "payment", indexes = {
    @Index(name = "ix_pay_ccd_case_number", columnList = "ccd_case_number")
})
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "external_reference")
    private String externalReference;

    @Column(name = "external_provider")
    private String externalProvider;

    @CreationTimestamp
    @Column(name = "date_created")
    private Date dateCreated;

    @UpdateTimestamp
    @Column(name =  "date_updated")
    private Date dateUpdated;

    @Transient
    private String email;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "description")
    private String description;

    @Column(name = "site_id")
    private String siteId;

    @Column(name = "giro_slip_no")
    private String giroSlipNo;

    @Transient
    private String status;
    @Transient
    private Boolean finished;
    @Transient
    private String returnUrl;
    @Transient
    private String nextUrl;
    @Transient
    private String cancelUrl;
    @Transient
    private String refundsUrl;

    @Column(name = "currency")
    private String currency;

    @Column(name = "ccd_case_number")
    private String ccdCaseNumber;

    @Column(name = "case_reference")
    private String caseReference;

    @Column(name = "service_type")
    private String serviceType;

    @ManyToOne
    @JoinColumn(name = "payment_channel")
    private PaymentChannel paymentChannel;

    @ManyToOne
    @JoinColumn(name = "payment_method")
    private PaymentMethod paymentMethod;

    @ManyToOne
    @JoinColumn(name = "payment_provider")
    private PaymentProvider paymentProvider;

    @ManyToOne
    @JoinColumn(name = "payment_status")
    private PaymentStatus paymentStatus;

    @Column(name = "organisation_name")
    private String organisationName;

    @Column(name = "pba_number")
    private String pbaNumber;

    @Column(name = "customer_reference")
    private String customerReference;

    @Column(name = "reference")
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_link_id", insertable = false, updatable = false)
    private PaymentFeeLink paymentLink;


    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "payment_id", referencedColumnName = "id", nullable = false)
    private List<StatusHistory> statusHistories;

}
