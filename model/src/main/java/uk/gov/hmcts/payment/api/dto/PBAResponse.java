package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;


@NoArgsConstructor
@AllArgsConstructor
@Builder(builderMethodName = "pbaDtoWith")
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
public class PBAResponse{
    private OrganisationEntityResponse organisationEntityResponse;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
    @JsonInclude(NON_NULL)
    public static class OrganisationEntityResponse{
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

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
    @JsonInclude(NON_NULL)
    public static class SuperUser{
        public String firstName;
        public String lastName;
        public String email;
    }
}






