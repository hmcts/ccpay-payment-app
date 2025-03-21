package uk.gov.hmcts.payment.api.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Entity
@Data
@Builder(builderMethodName = "paymentStatusWith")
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "payment_status")
public class PaymentStatus {

    public final static PaymentStatus CREATED = new PaymentStatus("created", "created");
    public final static PaymentStatus SUCCESS = new PaymentStatus("success", "success");
    public final static PaymentStatus CANCELLED = new PaymentStatus("cancelled", "cancelled");
    public final static PaymentStatus PENDING = new PaymentStatus("pending", "pending");
    public final static PaymentStatus ERROR = new PaymentStatus("error", "error");
    public final static PaymentStatus FAILED = new PaymentStatus("failed", "failed");

    @Id
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;
}
