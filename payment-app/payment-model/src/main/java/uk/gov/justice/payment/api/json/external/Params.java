
package uk.gov.justice.payment.api.json.external;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "chargeTokenId"
})
public class Params {

    @JsonProperty("chargeTokenId")
    private String chargeTokenId;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * 
     * @return
     *     The chargeTokenId
     */
    @JsonProperty("chargeTokenId")
    public String getChargeTokenId() {
        return chargeTokenId;
    }

    /**
     * 
     * @param chargeTokenId
     *     The chargeTokenId
     */
    @JsonProperty("chargeTokenId")
    public void setChargeTokenId(String chargeTokenId) {
        this.chargeTokenId = chargeTokenId;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
