
package uk.gov.justice.payment.api.json;

import com.fasterxml.jackson.annotation.*;

import javax.annotation.Generated;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
        "next_url",
        "next_url_post",
})
public class LinksInternal {


    @JsonProperty("next_url")
    private NextUrl nextUrl;
    @JsonProperty("next_url_post")
    private NextUrlPost nextUrlPost;


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



}
