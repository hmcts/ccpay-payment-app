package uk.gov.hmcts.payment.api.dto.liberata.identity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class AccessTokenDto {

    @JsonProperty("name")
    private String name;

    @JsonProperty("abilities")
    private List<String> abilities;

    @JsonProperty("expires_at")
    private String expiresAt;

    @JsonProperty("tokenable_id")
    private Integer tokenableId;

    @JsonProperty("tokenable_type")
    private String tokenableType;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("id")
    private Integer id;
}

