package uk.gov.hmcts.payment.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder(builderMethodName = "searchCriteriaWith")
public class PaymentSearchCriteria {

    private Date startDate;

    private Date endDate;

    private String paymentMethod;

    private String serviceType;

    private String ccdCaseNumber;

    private String pbaNumber;

    private String paymentStatus;

}
