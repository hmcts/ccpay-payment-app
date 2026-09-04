package uk.gov.hmcts.payment.api.dto.liberata.identity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class TokenResponse {

    private String accessToken;
    private long expiresIn;
    private long createdAt;

    /**
     * Checks if the token is expired.
     * All date comparisons are handled as `long` values representing epoch milliseconds.
     */
    public boolean isExpired() {
        return System.currentTimeMillis() >= (createdAt + expiresIn);
    }
}
