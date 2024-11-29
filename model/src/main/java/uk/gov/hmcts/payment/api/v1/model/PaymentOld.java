package uk.gov.hmcts.payment.api.v1.model;

import java.util.Date;
import jakarta.persistence.*;

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
@Table(name = "payment_old")
public class PaymentOld {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String userId;
    private String govPayId;
    @CreationTimestamp
    private Date dateCreated;

    @Transient
    private String email;
    @Transient
    private Integer amount;
    @Transient
    private String reference;
    @Transient
    private String description;
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
}
