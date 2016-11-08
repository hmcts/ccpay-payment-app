

package uk.gov.justice.payment.api.controllers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatePaymentRequestDto {
    private Integer amount;
    private String applicationReference;
    private String paymentReference;
    private String description;
    private String returnUrl;
    private String email;

    public boolean isValid() {
        if (getAmount() == null || "".equals(getAmount())) {
            return false;
        }
        if (getDescription() == null || "".equals(getDescription())) {
            return false;
        }
        if (getApplicationReference() == null || "".equals(getApplicationReference())) {
            return false;
        }
        if (getPaymentReference() == null || "".equals(getPaymentReference())) {
            return false;
        }

        if (getReturnUrl() == null || "".equals(getReturnUrl())) {
            return false;
        }

        if (getReturnUrl() != null && getReturnUrl().startsWith("http://")) {
            return false;
        }
        return true;
    }

    public String getValidationMessage() {
        String prefix = "attribute ";
        String postfix = " is mandatory. Please provide a valid value.";

        if (getAmount() == null || "".equals(getAmount())) {
            return prefix + "amount" + postfix;
        }
        if (getDescription() == null || "".equals(getDescription())) {
            return prefix + "description" + postfix;
        }
        if (getApplicationReference() == null || "".equals(getApplicationReference())) {
            return prefix + "application_reference" + postfix;
        }
        if (getPaymentReference() == null || "".equals(getPaymentReference())) {
            return prefix + "payment_reference" + postfix;
        }

        if (getReturnUrl() == null || "".equals(getReturnUrl())) {
            return prefix + "return_url" + postfix;
        }

        if (getReturnUrl() != null && getReturnUrl().startsWith("http://")) {
            return "return_url must be https";
        }
        return "";
    }

}
