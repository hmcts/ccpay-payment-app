package uk.gov.hmcts.payment.api.dto.liberata.identity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class TokenDto {

    @JsonProperty("accessToken")
    private AccessTokenDto accessToken;

    @JsonProperty("plainTextToken")
    private String plainTextToken;
}

