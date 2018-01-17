package uk.gov.hmcts.payment.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@Builder(builderMethodName = "feeWith")
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "fee")
public class Fee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code")
    private String code;

    @Column(name = "version")
    private String version;

    @Column(name = "amount")
    private Integer amount;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "payment_link_id", referencedColumnName = "id", nullable = false)
//    private PaymentLink paymentLink;
}
