package uk.gov.hmcts.payment.smoke.idam;

import com.fasterxml.jackson.annotation.JsonProperty;
import feign.Body;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

public interface IdamApi {

    @RequestLine("POST /o/token")
    @Headers("Content-Type: application/x-www-form-urlencoded")
    @Body("username={username}&password={password}&scope={scope}&grant_type="
        + "{grant_type}&client_id={client_id}&client_secret={client_secret}&redirect_uri={redirect_uri}")
    TokenExchangeResponse exchangeCode(@Param("username") String username,
                                       @Param("password") String password,
                                       @Param("scope") String scope,
                                       @Param("grant_type") String grantType,
                                       @Param("client_id") String clientId,
                                       @Param("client_secret") String clientSecret,
                                       @Param("redirect_uri") String redirectUri);

    @Data
    class TokenExchangeResponse {
        @JsonProperty("access_token")
        private String accessToken;
    }
}
