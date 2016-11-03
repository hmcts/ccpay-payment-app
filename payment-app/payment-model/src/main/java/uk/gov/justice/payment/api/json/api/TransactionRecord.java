
package uk.gov.justice.payment.api.json.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.payment.api.json.AbstractDomainObject;

import javax.annotation.Generated;

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
@Builder(builderMethodName = "transactionRecordWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TransactionRecord extends AbstractDomainObject {

    private Integer amount;
    @JsonProperty("application_reference")
    private String applicationReference;
    @JsonProperty("created_date")
    private String createdDate;
    private String description;
    @JsonProperty("payment_id")
    private String paymentId;
    @JsonProperty("payment_reference")
    private String paymentReference;
    @JsonProperty("service_id")
    private String serviceId;
    private String email;
}
