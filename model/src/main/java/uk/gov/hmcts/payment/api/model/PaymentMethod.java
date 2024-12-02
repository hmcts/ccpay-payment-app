package uk.gov.hmcts.payment.api.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@Builder(builderMethodName = "paymentMethodWith")
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "payment_method")
public class PaymentMethod {

    @Id
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;
}
