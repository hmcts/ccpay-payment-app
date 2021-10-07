package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;
import org.hibernate.validator.constraints.Email;
import javax.validation.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.Min;
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
    @Min(1)
    private Integer amount;
    @NotEmpty
    private String reference;
    @NotEmpty
    private String description;

    @NotEmpty
    @URL(protocol = "https")
    private String returnUrl;
}
