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
@Builder(builderMethodName = "telephonyCallbackWith")
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "telephony_callback")
public class TelephonyCallback {

    @Id
    @Column(name = "payment_reference", nullable = false)
    private String paymentReference;

    @Column(name = "payload")
    private String payload;
}
