package uk.gov.justice.payment.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.util.Date;

@Entity(name = "paymentHistory")
@Data
@Builder(builderMethodName = "paymentHistoryEntryWith")
@AllArgsConstructor
@NoArgsConstructor
public class PaymentHistoryEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    private Payment payment;

    @CreationTimestamp
    private Date timestamp;
    private String action;
    private String status;
    private Boolean finished;
    private String govPayJson;
}
