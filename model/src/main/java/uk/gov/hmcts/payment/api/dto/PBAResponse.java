package uk.gov.hmcts.payment.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(builderMethodName = "pbaDtoWith")
public class PBAResponse{
    public OrganisationEntityResponse organisationEntityResponse;
}

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


