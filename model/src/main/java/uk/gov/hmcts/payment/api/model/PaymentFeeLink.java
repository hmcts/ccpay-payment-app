package uk.gov.hmcts.payment.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;



@Entity
@Data
@Builder(builderMethodName = "paymentFeeLinkWith")
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "payment_fee_link")
public class PaymentFeeLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "payment_reference")
    private String paymentReference;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "payment_link_id", referencedColumnName = "id", nullable = false)
    private List<Payment> payments;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "payment_link_id", referencedColumnName = "id", nullable = false)
    private List<Fee> fees;
}
