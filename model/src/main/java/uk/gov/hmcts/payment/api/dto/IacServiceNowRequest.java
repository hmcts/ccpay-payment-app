package uk.gov.hmcts.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import lombok.experimental.Wither;

import javax.persistence.Column;
import javax.validation.Valid;
import java.util.List;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@With
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderMethodName = "createIacServiceNowRequestWith")
@Wither
public class IacServiceNowRequest {

    @Valid
    private String callerId;
    private  String contactType;
    private String serviceOffering;
    private String category;
    private String subcategory;
    private String assignmentGroup;
    private String shortDescription;
    private String description;
    @JsonProperty("u_role_type")
    private String uRoleType;
    private String impact;
    private String urgency;

}
