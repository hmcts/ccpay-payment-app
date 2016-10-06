
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.justice.payment.api.json.AbstractDomainObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "amount",
    "state",
    "description",
    "reference",
    "payment_id",
    "payment_provider",
    "return_url",
    "created_date",
    "refund_summary",
    "_links"
})
public class GDSCreatePaymentResponse extends AbstractDomainObject {

    @JsonProperty("amount")
    private Integer amount;
    @JsonProperty("state")
    private State state;
    @JsonProperty("description")
    private String description;
    @JsonProperty("reference")
    private String reference;
    @JsonProperty("payment_id")
    private String paymentId;
    @JsonProperty("payment_provider")
    private String paymentProvider;
    @JsonProperty("return_url")
    private String returnUrl;
    @JsonProperty("created_date")
    private String createdDate;
    @JsonProperty("refund_summary")
    private RefundSummary refundSummary;
    @JsonProperty("_links")
    private Links links;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * 
     * @return
     *     The amount
     */
    @JsonProperty("amount")
    public Integer getAmount() {
        return amount;
    }

    /**
     * 
     * @param amount
     *     The amount
     */
    @JsonProperty("amount")
    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    /**
     * 
     * @return
     *     The state
     */
    @JsonProperty("state")
    public State getState() {
        return state;
    }

    /**
     * 
     * @param state
     *     The state
     */
    @JsonProperty("state")
    public void setState(State state) {
        this.state = state;
    }

    /**
     * 
     * @return
     *     The description
     */
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    /**
     * 
     * @param description
     *     The description
     */
    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 
     * @return
     *     The reference
     */
    @JsonProperty("reference")
    public String getReference() {
        return reference;
    }

    /**
     * 
     * @param reference
     *     The reference
     */
    @JsonProperty("reference")
    public void setReference(String reference) {
        this.reference = reference;
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
     *     The paymentProvider
     */
    @JsonProperty("payment_provider")
    public String getPaymentProvider() {
        return paymentProvider;
    }

    /**
     * 
     * @param paymentProvider
     *     The payment_provider
     */
    @JsonProperty("payment_provider")
    public void setPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    /**
     * 
     * @return
     *     The returnUrl
     */
    @JsonProperty("return_url")
    public String getReturnUrl() {
        return returnUrl;
    }

    /**
     * 
     * @param returnUrl
     *     The return_url
     */
    @JsonProperty("return_url")
    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    /**
     * 
     * @return
     *     The createdDate
     */
    @JsonProperty("created_date")
    public String getCreatedDate() {
        return createdDate;
    }

    /**
     * 
     * @param createdDate
     *     The created_date
     */
    @JsonProperty("created_date")
    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * 
     * @return
     *     The refundSummary
     */
    @JsonProperty("refund_summary")
    public RefundSummary getRefundSummary() {
        return refundSummary;
    }

    /**
     * 
     * @param refundSummary
     *     The refund_summary
     */
    @JsonProperty("refund_summary")
    public void setRefundSummary(RefundSummary refundSummary) {
        this.refundSummary = refundSummary;
    }

    /**
     * 
     * @return
     *     The links
     */
    @JsonProperty("_links")
    public Links getLinks() {
        return links;
    }

    /**
     * 
     * @param links
     *     The _links
     */
    @JsonProperty("_links")
    public void setLinks(Links links) {
        this.links = links;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public String toString() {
        return super.toString(this);
    }
}
