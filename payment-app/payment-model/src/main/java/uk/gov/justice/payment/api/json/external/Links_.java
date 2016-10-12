
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
    "self",
    "last_page",
    "first_page"
})
public class Links_ {

    @JsonProperty("self")
    private Self_ self;
    @JsonProperty("last_page")
    private LastPage lastPage;
    @JsonProperty("first_page")
    private FirstPage firstPage;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * 
     * @return
     *     The self
     */
    @JsonProperty("self")
    public Self_ getSelf() {
        return self;
    }

    /**
     * 
     * @param self
     *     The self
     */
    @JsonProperty("self")
    public void setSelf(Self_ self) {
        this.self = self;
    }

    /**
     * 
     * @return
     *     The lastPage
     */
    @JsonProperty("last_page")
    public LastPage getLastPage() {
        return lastPage;
    }

    /**
     * 
     * @param lastPage
     *     The last_page
     */
    @JsonProperty("last_page")
    public void setLastPage(LastPage lastPage) {
        this.lastPage = lastPage;
    }

    /**
     * 
     * @return
     *     The firstPage
     */
    @JsonProperty("first_page")
    public FirstPage getFirstPage() {
        return firstPage;
    }

    /**
     * 
     * @param firstPage
     *     The first_page
     */
    @JsonProperty("first_page")
    public void setFirstPage(FirstPage firstPage) {
        this.firstPage = firstPage;
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
