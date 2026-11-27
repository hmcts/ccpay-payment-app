package uk.gov.hmcts.payment.api.dto.servicerequest;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;
import java.util.TreeSet;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static uk.gov.hmcts.payment.api.domain.service.ServiceRequestCancellationErrors.MAX_CANCELLATION_REASON_LENGTH;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "cancelDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

@Schema(description = "Optional for an automated cancellation. Fields other than reason are rejected with 400.")
public class ServiceRequestCancelDto {
    // Automated cancellations - body can be null.
    // For manual - this is where the reason would be supplied
    @JsonProperty("reason")
    @Schema(description = "Why the service request is being cancelled. Required for a manual cancellation. For an "
        + "automated cancellation it may be omitted with the whole body, and defaults to \"Auto-Cancelled by Service\".",
        maxLength = MAX_CANCELLATION_REASON_LENGTH, example = "Duplicate service request")
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
