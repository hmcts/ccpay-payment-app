
package uk.gov.justice.payment.api.json.api;

import com.fasterxml.jackson.annotation.*;
import org.springframework.http.HttpMethod;
import uk.gov.justice.payment.api.json.external.Cancel;
import uk.gov.justice.payment.api.json.external.GDSCreatePaymentResponse;
import uk.gov.justice.payment.api.json.external.LinksInternal;

import javax.annotation.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({

    "payment_id",
        "_links"
})
public class CreatePaymentResponse {




    @JsonProperty("payment_id")
    private String paymentId;
    @JsonProperty("_links")
    private LinksInternal links;


    public CreatePaymentResponse(){}
    public CreatePaymentResponse(GDSCreatePaymentResponse response,String url){
        setPaymentId(response.getPaymentId());
        LinksInternal linksInternal = new LinksInternal();
        linksInternal.setNextUrl(response.getLinks().getNextUrl());
        Cancel cancel = new Cancel();
        cancel.setHref(url+"/"+response.getPaymentId()+"/cancel");
        cancel.setMethod(HttpMethod.POST.toString());
        linksInternal.setCancelUrl(cancel);
        setLinks(linksInternal);
    }


    /**
     * 
     * @return
     *     The paymentId
     */
    @JsonProperty("payment_id")
    public String getPaymentId() {
        return paymentId;
    }

    /**
     * 
     * @param paymentId
     *     The payment_id
     */
    @JsonProperty("payment_id")
    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }
    /**
     *
     * @return
     *     The links
     */
    @JsonProperty("_links")
    public LinksInternal getLinks() {
        return links;
    }

    /**
     *
     * @param links
     *     The _links
     */
    @JsonProperty("_links")
    public void setLinks(LinksInternal links) {
        this.links = links;
    }



}
