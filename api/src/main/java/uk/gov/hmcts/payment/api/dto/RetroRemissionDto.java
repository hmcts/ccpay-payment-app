package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import lombok.*;
import org.springframework.hateoas.RepresentationModel;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "retroRemissionDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RetroRemissionDto extends RepresentationModel<RetroRemissionDto> {
    private String remissionReference;
}
