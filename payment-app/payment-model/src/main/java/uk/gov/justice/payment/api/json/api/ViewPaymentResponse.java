
package uk.gov.justice.payment.api.json.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.payment.api.json.AbstractDomainObject;
import uk.gov.justice.payment.api.json.external.GDSViewPaymentResponse;
import uk.gov.justice.payment.api.json.external.State;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "amount",
        "state",
        "description",
        "reference",
        "payment_id",
        "created_date"
})
@Builder(builderMethodName = "viewPaymentResponseWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ViewPaymentResponse extends AbstractDomainObject {

    private Integer amount;
    private State state;
    private String description;
    private String reference;
    @JsonProperty("payment_id")
    private String paymentId;
    @JsonProperty("created_date")
    private String createdDate;

    public ViewPaymentResponse(GDSViewPaymentResponse gdsViewPaymentResponse) {
        this.amount = gdsViewPaymentResponse.getAmount();
        this.state = gdsViewPaymentResponse.getState();
        this.description = gdsViewPaymentResponse.getDescription();
        this.reference = gdsViewPaymentResponse.getReference();
        this.paymentId = gdsViewPaymentResponse.getPaymentId();
        this.createdDate = gdsViewPaymentResponse.getCreatedDate();
    }
}
