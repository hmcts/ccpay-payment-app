package uk.gov.hmcts.payment.api.dto.liberata;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.util.Date;
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
    @JsonFormat(
        shape = JsonFormat.Shape.STRING,
        pattern = "yyyy-MM-dd HH:mm:ss",
        timezone = "GMT"
    )
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

