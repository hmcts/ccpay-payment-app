package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@Builder(builderMethodName = "organisationEntityResponseDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrganisationEntityResponse {

    private String name;
    private String organisationIdentifier;
    private String status;
    private String sraId;
    private boolean sraRegulated;
    private String companyNumber;
    private String companyUrl;
    private SuperUser superUser;
    private List<String> paymentAccount;
}
