package uk.gov.hmcts.payment.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Entity
@Data
@Builder(builderMethodName = "statusHistoryWith")
@AllArgsConstructor
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "payment_event_history")
public class StatusHistoryNoUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @CreationTimestamp
    @Column(name = "date_created")
    private Date dateCreated;

    @UpdateTimestamp
    @Column(name = "date_updated")
    private Date dateUpdated;

    @Column(name = "status")
    private String status;

    @Column(name = "external_status")
    private String externalStatus;

    @Column(name = "errorCode")
    private String errorCode;

    @Column(name = "message")
    private String message;

    @ManyToOne
    @JoinColumn(name = "event_name")
    private PaymentEvent eventName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", insertable = false, updatable = false)
    private PaymentNoUpdate payment;

    public static List<StatusHistoryNoUpdate> fromStatusHistoryUpdateList(List<StatusHistory> statusHistoryList) {
        Function<StatusHistory, StatusHistoryNoUpdate> externalToMyLocation
            = statusHistory -> StatusHistoryNoUpdate.statusHistoryWith()
                .id(statusHistory.getId())
                .dateCreated(statusHistory.getDateCreated())
                .dateUpdated(statusHistory.getDateUpdated())
                .status(statusHistory.getStatus())
                .externalStatus(statusHistory.getExternalStatus())
                .errorCode(statusHistory.getErrorCode())
                .message(statusHistory.getMessage())
                .eventName(statusHistory.getEventName())
                .payment(PaymentNoUpdate.fromPayment(statusHistory.getPayment()))
                .build();

        return statusHistoryList.stream()
            .map(externalToMyLocation)
            .collect(Collectors.toList());
    }
}
