package uk.gov.hmcts.payment.api.model;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import uk.gov.hmcts.payment.api.jpaaudit.listner.Auditable;
import uk.gov.hmcts.payment.api.jpaaudit.listner.PaymentEntityListener;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Entity
@EntityListeners(PaymentEntityListener.class)
@Data
@Builder(builderMethodName = "paymentWith")
@AllArgsConstructor
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "payment", indexes = {
    @Index(name = "ix_pay_ccd_case_number", columnList = "ccd_case_number"),
    @Index(name = "ix_pay_payment_status_provider", columnList = "payment_status, payment_provider")
})
public class Payment extends Auditable<String> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "external_reference")
    private String externalReference;

    @CreationTimestamp
    @Column(name = "date_created")
    private Date dateCreated;

    @UpdateTimestamp
    @Column(name = "date_updated")
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

    @Column(name = "s2s_service_name")
    private String s2sServiceName;

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

    @Column(name = "reported_date_offline")
    private Date reportedDateOffline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_link_id", insertable = false, updatable = false)
    @ToString.Exclude
    private PaymentFeeLink paymentLink;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "payment_id", referencedColumnName = "id", nullable = false)
    @ToString.Exclude
    private List<StatusHistory> statusHistories;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "payment_id", referencedColumnName = "id", nullable = false)
    private List<PaymentAllocation> paymentAllocation;

    @Column(name = "service_callback_url")
    private String serviceCallbackUrl;

    @Column(name = "document_control_number")
    private String documentControlNumber;

    @Column(name = "banked_date")
    private Date bankedDate;

    @Column(name = "payer_name")
    private String payerName;

}
