

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
        import uk.gov.justice.payment.api.json.api.CreatePaymentRequest;

        @JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
        "amount",
        "reference",
        "description",
        "return_url"
})
public class GDSCreatePaymentRequest {

    @JsonProperty("amount")
    private Integer amount;
    @JsonProperty("reference")
    private String reference;
    @JsonProperty("description")
    private String description;
    @JsonProperty("return_url")
    private String returnUrl;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public GDSCreatePaymentRequest(){}

    public GDSCreatePaymentRequest(CreatePaymentRequest request) {
        this.setAmount(request.getAmount());
        this.setReference(request.getPaymentReference());
        this.setDescription(request.getDescription());
        this.setReturnUrl(request.getReturnUrl());
    }

    /**
     *
     * @return
     * The amount
     */
    @JsonProperty("amount")
    public Integer getAmount() {
        return amount;
    }

    /**
     *
     * @param amount
     * The amount
     */
    @JsonProperty("amount")
    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    /**
     *
     * @return
     * The reference
     */
    @JsonProperty("reference")
    public String getReference() {
        return reference;
    }

    /**
     *
     * @param reference
     * The reference
     */
    @JsonProperty("reference")
    public void setReference(String reference) {
        this.reference = reference;
    }

    /**
     *
     * @return
     * The description
     */
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    /**
     *
     * @param description
     * The description
     */
    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     *
     * @return
     * The returnUrl
     */
    @JsonProperty("return_url")
    public String getReturnUrl() {
        return returnUrl;
    }

    /**
     *
     * @param returnUrl
     * The return_url
     */
    @JsonProperty("return_url")
    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
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
        return super.toString();
    }


}