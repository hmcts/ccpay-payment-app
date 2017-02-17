package uk.gov.justice.payment.api.model;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;
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
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String serviceId;
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
