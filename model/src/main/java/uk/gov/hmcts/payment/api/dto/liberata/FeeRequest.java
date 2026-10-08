package uk.gov.hmcts.payment.api.dto.liberata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonInclude(NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder(builderMethodName = "feeWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class FeeRequest {
    private Integer id;
    private String code;
    private String version;
    private String volume;
    private String calculatedAmount;
    private String memoline;
    private String nac;
    private String jurisdiction1;
    private String jurisdiction2;
}

