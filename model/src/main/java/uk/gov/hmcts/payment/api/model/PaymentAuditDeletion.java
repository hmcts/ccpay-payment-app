package uk.gov.hmcts.payment.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

@Entity
@Data
@Builder(builderMethodName = "paymentAuditDeletionWith")
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "payment_audit_deletion")
public class PaymentAuditDeletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ccd_case_number", nullable = false)
    private String ccdCaseNumber;

    @Column(name = "source_system", nullable = false)
    private String sourceSystem;

    @CreationTimestamp
    @Column(name = "deletion_date", nullable = false)
    private Date deletionDate;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "success", nullable = false)
    private Boolean success;
}
