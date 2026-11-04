package uk.gov.hmcts.payment.api.dto.liberata;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder(builderMethodName = "paymentDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PaymentRequest {
    private String serviceName;
    private String groupReference;
    private String paymentReference;
    private LocalDate dateCreated;
    private String amount;
    private String currency;
    private String siteId;
    private String caseReference;
    private String ccdCaseNumber;
    private String customerReference;
    private String surname;
    private List<FeeRequest> fee;
}

