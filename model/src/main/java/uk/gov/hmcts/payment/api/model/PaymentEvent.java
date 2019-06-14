package uk.gov.hmcts.payment.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Data
@Builder(builderMethodName = "paymentEventWith")
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "payment_event")
public class PaymentEvent {

    public final static PaymentEvent CASE_REF_UPDATE = new PaymentEvent("CASE_REF_UPDATE", "Payment case reference has been updated");
    public final static PaymentEvent STATUS_CHANGE = new PaymentEvent("STATUS_CHANGE", "Payment status has been changed");

    @Id
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;
}
