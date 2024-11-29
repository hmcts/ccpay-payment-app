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
