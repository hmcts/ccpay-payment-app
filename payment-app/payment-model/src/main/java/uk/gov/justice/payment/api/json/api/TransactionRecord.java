
package uk.gov.justice.payment.api.json.api;

import com.fasterxml.jackson.annotation.*;
import uk.gov.justice.payment.api.json.AbstractDomainObject;
import uk.gov.justice.payment.api.json.external.GDSViewPaymentResponse;
import uk.gov.justice.payment.api.json.external.State;

import javax.annotation.Generated;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
        "amount",
        "application_reference",
        "created_date",
        "description",
        "payment_id",
        "payment_reference",
        "service_id",
        "email",
})
public class TransactionRecord extends AbstractDomainObject  {

    @JsonProperty("amount")
    private Integer amount;
    @JsonProperty("application_reference")
    private String applicationReference;
    @JsonProperty("created_date")
    private String createdDate;
    @JsonProperty("description")
    private String description;
    @JsonProperty("payment_id")
    private String paymentId;
    @JsonProperty("payment_reference")
    private String paymentReference;
    @JsonProperty("service_id")
    private String serviceId;
    @JsonProperty("email")
    private String email;


    public TransactionRecord(){}

    public TransactionRecord( Integer amount,
                               String application_reference,
                               String createdDate,
                               String description,
                               String paymentId,
                               String paymentReference,
                               String serviceId,
                               String email ) {
        this.amount=amount;
        this.applicationReference=application_reference;
        this.createdDate=createdDate;
        this.description=description;
        this.paymentId=paymentId;
        this.paymentReference=paymentReference;
        this.serviceId=serviceId;
        this.email=email;
    }




    @JsonProperty("application_reference")
    public String getApplicationReference() {
        return applicationReference;
    }
    @JsonProperty("application_reference")
    public void setApplicationReference(String applicationReference) {
        this.applicationReference = applicationReference;
    }
    @JsonProperty("payment_reference")
    public String getPaymentReference() {
        return paymentReference;
    }
    @JsonProperty("payment_reference")
    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }
    @JsonProperty("service_id")
    public String getServiceId() {
        return serviceId;
    }
    @JsonProperty("service_id")
    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }


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
     *     The email
     */
    @JsonProperty("email")
    public String getEmail() {
        return email;
    }

    /**
     *
     * @param email
     *     The email
     */
    @JsonProperty("email")
    public void setEmail(String email) {
        this.email = email;
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




    @Override
    public String toString() {
        return super.toString(this);
    }
}
