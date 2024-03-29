package uk.gov.hmcts.payment.api.dto.servicerequest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;


import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "deadLetterDtoWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class DeadLetterDto {
    private String action;
    private String caseId;
    private String orderReference;
    private String responsibleParty;

}
