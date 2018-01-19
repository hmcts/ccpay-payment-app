package uk.gov.hmcts.payment.api.model;

import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Data
@Builder(builderMethodName = "paymentWith")
@AllArgsConstructor
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "payment")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "gov_pay_id")
    private String govPayId;

    @CreationTimestamp
    @Column(name = "date_created")
    private Date dateCreated;

    @Transient
    private String email;

    @Column(name = "amount")
    private BigDecimal amount;

    @Transient
    private String reference;

    @Column(name = "description")
    private String description;

    @Column(name = "site_id")
    private String siteId;

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

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "payment_link_id", referencedColumnName = "id", nullable = false)
//    private PaymentLink paymentLink;

}
