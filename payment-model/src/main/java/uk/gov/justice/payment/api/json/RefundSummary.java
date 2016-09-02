
package uk.gov.justice.payment.api.json;

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
    "status",
    "amount_available",
    "amount_submitted"
})
public class RefundSummary {

    @JsonProperty("status")
    private String status;
    @JsonProperty("amount_available")
    private Integer amountAvailable;
    @JsonProperty("amount_submitted")
    private Integer amountSubmitted;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * 
     * @return
     *     The status
     */
    @JsonProperty("status")
    public String getStatus() {
        return status;
    }

    /**
     * 
     * @param status
     *     The status
     */
    @JsonProperty("status")
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 
     * @return
     *     The amountAvailable
     */
    @JsonProperty("amount_available")
    public Integer getAmountAvailable() {
        return amountAvailable;
    }

    /**
     * 
     * @param amountAvailable
     *     The amount_available
     */
    @JsonProperty("amount_available")
    public void setAmountAvailable(Integer amountAvailable) {
        this.amountAvailable = amountAvailable;
    }

    /**
     * 
     * @return
     *     The amountSubmitted
     */
    @JsonProperty("amount_submitted")
    public Integer getAmountSubmitted() {
        return amountSubmitted;
    }

    /**
     * 
     * @param amountSubmitted
     *     The amount_submitted
     */
    @JsonProperty("amount_submitted")
    public void setAmountSubmitted(Integer amountSubmitted) {
        this.amountSubmitted = amountSubmitted;
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
