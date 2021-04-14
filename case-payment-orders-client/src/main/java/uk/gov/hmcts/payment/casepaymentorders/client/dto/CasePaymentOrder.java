package uk.gov.hmcts.payment.casepaymentorders.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CasePaymentOrder implements Serializable {
    private static final long serialVersionUID = -6558703031023866825L;

    private UUID id;
    private LocalDateTime createdTimestamp;
    private Long caseId;
    private String action;
    private String responsibleParty;
    private String orderReference;
    private String createdBy;
}
