
package uk.gov.justice.payment.api.json.external;

import com.fasterxml.jackson.annotation.*;

import javax.annotation.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
        "next_url",
        "cancel_url"
})
public class LinksInternal {


    @JsonProperty("next_url")
    private NextUrl nextUrl;
    @JsonProperty("cancel_url")
    private Cancel cancelUrl;


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
     *     The cancelUrlPost
     */
    @JsonProperty("cancel_url")
    public Cancel getCancelUrl() {
        return cancelUrl;
    }

    /**
     *
     * @param cancelUrl
     *     The cancel_url
     */
    @JsonProperty("cancel_url")
    public void setCancelUrl(Cancel cancelUrl) {
        this.cancelUrl = cancelUrl;
    }



}
