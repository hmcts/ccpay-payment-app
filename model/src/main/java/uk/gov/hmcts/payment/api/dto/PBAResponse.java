package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@AllArgsConstructor
@Getter
//@JsonIgnoreProperties(ignoreUnknown = true)
class SuperUser{
    public String firstName;
    public String lastName;
    public String email;
}

 class OrganisationEntityResponse{
    public String name;
    public String organisationIdentifier;
    public String status;
    public String sraId;
    public boolean sraRegulated;
    public String companyNumber;
    public String companyUrl;
    public SuperUser superUser;
    public List<String> paymentAccount;
}

public class PBAResponse{
    public OrganisationEntityResponse organisationEntityResponse;
}
