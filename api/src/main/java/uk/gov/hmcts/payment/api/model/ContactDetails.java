package uk.gov.hmcts.payment.api.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(builderMethodName = "contactDetailsWith")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ContactDetails {

    private String addressLine;

    private String country;

    private String county;

    private String city;

    private String postalCode;

    private String email;

    private String notificationType;
}
