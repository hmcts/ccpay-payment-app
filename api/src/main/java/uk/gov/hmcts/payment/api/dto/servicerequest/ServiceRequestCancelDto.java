package uk.gov.hmcts.payment.api.dto.servicerequest;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.util.Set;
import java.util.TreeSet;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "cancelDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ServiceRequestCancelDto {
    // Automated cancellations - body can be null.
    // For manual - this is where the reason would be supplied
    @JsonProperty("reason")
    private String reason;

    // The request schema disallows additional properties, but the application's ObjectMapper ignores unknown
    // fields. They are collected here so the controller can reject them with the cancellation error body,
    // rather than failing deserialisation, which would produce an empty 400 from ControllerExceptionHandler.
    @JsonIgnore
    @Builder.Default
    private Set<String> unknownProperties = new TreeSet<>();

    @JsonAnySetter
    public void addUnknownProperty(String name, Object value) {
        unknownProperties.add(name);
    }
}
