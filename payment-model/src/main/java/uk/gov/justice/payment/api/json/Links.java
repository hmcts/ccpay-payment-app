
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
        "self",
        "next_url",
        "next_url_post",
        "events",
        "refunds",
        "cancel"
})
public class Links {

    @JsonProperty("self")
    private Self self;
    @JsonProperty("next_url")
    private NextUrl nextUrl;
    @JsonProperty("next_url_post")
    private NextUrlPost nextUrlPost;
    @JsonProperty("events")
    private Events events;
    @JsonProperty("refunds")
    private Refunds refunds;
    @JsonProperty("cancel")
    private Cancel cancel;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     *
     * @return
     *     The self
     */
    @JsonProperty("self")
    public Self getSelf() {
        return self;
    }

    /**
     *
     * @param self
     *     The self
     */
    @JsonProperty("self")
    public void setSelf(Self self) {
        this.self = self;
    }

    /**
     *
     * @return
     *     The nextUrl
     */
    @JsonProperty("next_url")
    public NextUrl getNextUrl() {
        return nextUrl;
    }

    /**
     *
     * @param nextUrl
     *     The next_url
     */
    @JsonProperty("next_url")
    public void setNextUrl(NextUrl nextUrl) {
        this.nextUrl = nextUrl;
    }

    /**
     *
     * @return
     *     The nextUrlPost
     */
    @JsonProperty("next_url_post")
    public NextUrlPost getNextUrlPost() {
        return nextUrlPost;
    }

    /**
     *
     * @param nextUrlPost
     *     The next_url_post
     */
    @JsonProperty("next_url_post")
    public void setNextUrlPost(NextUrlPost nextUrlPost) {
        this.nextUrlPost = nextUrlPost;
    }

    /**
     *
     * @return
     *     The events
     */
    @JsonProperty("events")
    public Events getEvents() {
        return events;
    }

    /**
     *
     * @param events
     *     The events
     */
    @JsonProperty("events")
    public void setEvents(Events events) {
        this.events = events;
    }

    /**
     *
     * @return
     *     The refunds
     */
    @JsonProperty("refunds")
    public Refunds getRefunds() {
        return refunds;
    }

    /**
     *
     * @param refunds
     *     The refunds
     */
    @JsonProperty("refunds")
    public void setRefunds(Refunds refunds) {
        this.refunds = refunds;
    }

    /**
     *
     * @return
     *     The cancel
     */
    @JsonProperty("cancel")
    public Cancel getCancel() {
        return cancel;
    }

    /**
     *
     * @param cancel
     *     The cancel
     */
    @JsonProperty("cancel")
    public void setCancel(Cancel cancel) {
        this.cancel = cancel;
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
