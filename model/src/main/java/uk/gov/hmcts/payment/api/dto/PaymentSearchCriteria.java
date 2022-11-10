package uk.gov.hmcts.payment.api.dto;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.util.Date;

@ToString
@Data
@Builder(builderMethodName = "searchCriteriaWith")
public class PaymentSearchCriteria {

    private Date startDate;

    private Date endDate;

    private String paymentMethod;

    private String serviceType;

    private String ccdCaseNumber;

    private String pbaNumber;

}
