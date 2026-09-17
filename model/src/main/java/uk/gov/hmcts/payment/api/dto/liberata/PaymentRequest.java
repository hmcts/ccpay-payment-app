package uk.gov.hmcts.payment.api.dto.liberata;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.Date;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

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
    private Date dateCreated;
    private String amount;
    private String currency;
    private String siteId;
    private String caseReference;
    private String ccdCaseNumber;
    private String customerReference;
    private String surname;
    private List<FeeRequest> fee;
}

