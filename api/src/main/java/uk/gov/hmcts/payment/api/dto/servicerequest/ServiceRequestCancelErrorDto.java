package uk.gov.hmcts.payment.api.dto.servicerequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Builder(builderMethodName = "cancelErrorDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ServiceRequestCancelErrorDto {
    @JsonProperty("error")
    private String error;
}
