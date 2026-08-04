package uk.gov.hmcts.payment.api.dto.idam;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder(builderMethodName = "idamFullNameRetrivalResponseWith")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
public class IdamUserInfoResponse {

    private String id;
    private String forename;
    private String surname;
    private String email;
    private boolean active;
    private List<String> roles;
    private String lastModified;
    private String createDate;
    private boolean stale;
}
