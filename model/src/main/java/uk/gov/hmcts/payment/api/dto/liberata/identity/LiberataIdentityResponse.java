// File: `model/src/main/java/uk/gov/hmcts/payment/api/dto/liberata/identity/LiberataIdentityWrapper.java`
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
public class LiberataIdentityResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("data")
    private LiberataTokenData data;
}
