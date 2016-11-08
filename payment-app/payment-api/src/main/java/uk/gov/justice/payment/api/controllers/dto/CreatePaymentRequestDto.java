

package uk.gov.justice.payment.api.controllers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.NotNull;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderMethodName = "createPaymentRequestDtoWith")
@Wither
public class CreatePaymentRequestDto {
    @NotNull
    private Integer amount;
    @NotNull
    private String applicationReference;
    @NotNull
    private String paymentReference;
    @NotNull
    private String description;
    @NotNull
    @URL(protocol = "https")
    private String returnUrl;
    @Email
    @NotNull
    private String email;
}
