package uk.gov.hmcts.payment.api.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Data
@Builder(builderMethodName = "paymentAllocationStatusWith")
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "payment_allocation_status")
public class PaymentAllocationStatus {

    @Id
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;
}
