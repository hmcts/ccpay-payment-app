package uk.gov.justice.payment.api.services;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(builderMethodName = "searchCriteriaWith")
@AllArgsConstructor
@NoArgsConstructor
public class SearchCriteria {
    private Integer amount;
    private String paymentReference;
    private String applicationReference;
    private String serviceId;
    private String description;
    private String returnUrl;
    private String response;
    private String status;
    private String createdDate;
    private String email;
}
