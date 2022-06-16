package uk.gov.hmcts.payment.api.model;


import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Getter
@Setter
@ToString
@Builder(builderMethodName = "paymentFailuresWith")
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "payment_failures")
public class PaymentFailures {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ToString.Exclude
    @Column(name = "failure_reference", nullable = false)
    private String failureReference;

    @ToString.Exclude
    @Column(name = "reason", nullable = false)
    private String reason;

    @ToString.Exclude
    @Column(name = "payment_reference")
    private String paymentReference;

    @ToString.Exclude
    @Column(name = "ccd_case_number")
    private String ccdCaseNumber;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @ToString.Exclude
    @Column(name = "additional_reference")
    private String additionalReference;

    @ToString.Exclude
    @Column(name = "po_box_number")
    private String poBoxNumber;

    @ToString.Exclude
    @Column(name = "dcn")
    private String dcn;

    //@CreationTimestamp
    @Column(name = "failure_event_date_time", nullable = false)
    private Date failureEventDateTime;

    @Column(name = "has_amount_debited")
    private String hasAmountDebited;

    @Column(name = "representment_success")
    private String representmentSuccess;

    @Column(name = "representment_outcome_date")
    private Date representmentOutcomeDate;

    @ToString.Exclude
    @Column(name = "failure_type")
    private String failureType;

    @Override
    public int hashCode(){
        return super.hashCode();
    }
}
