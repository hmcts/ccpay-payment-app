package uk.gov.hmcts.payment.api.dto.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import javax.validation.constraints.NotBlank;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "serviceRequestCpoDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ServiceRequestCpoDto {

    @NotBlank
    private String action;

    @NotBlank
    private String case_id;

    @NotBlank
    private String order_reference;

    @NotBlank
    private String responsible_party;

}
