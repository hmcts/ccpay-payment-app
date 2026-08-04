package uk.gov.hmcts.payment.api.dto.idam;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
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
public class IdamTokenResponse {

    private String accessToken;

    private String refreshToken;

    private String scope;

    private String idToken;

    private String tokenType;

    private String expiresIn;
}
