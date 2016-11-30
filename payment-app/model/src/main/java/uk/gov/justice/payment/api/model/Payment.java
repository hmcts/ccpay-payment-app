package uk.gov.justice.payment.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.util.Date;

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
    private String email;
    private Integer amount;
    private String applicationReference;
    private String paymentReference;
    private String description;
    private String status;
    private Boolean finished;
    @CreationTimestamp
    private Date dateCreated;
    private String returnUrl;
    private String nextUrl;
    private String cancelUrl;
    private String refundsUrl;
}
