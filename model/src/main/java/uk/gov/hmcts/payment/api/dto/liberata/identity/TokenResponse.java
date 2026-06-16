package uk.gov.hmcts.payment.api.dto.liberata.identity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
public class TokenResponse {

    private String accessToken;
    private long expiresIn = 120 * 1000; //3 minutes
    private long createdAt = System.currentTimeMillis();

    public String getAccessToken() {
        return accessToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= (createdAt + expiresIn);
    }
}

