package uk.gov.hmcts.payment.api.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderMethodName = "ccdLinkWith")
@Data
public class CcdLink {
    String deprecation;
    String href;
    String hreflang;
    String media;
    String rel;
    String title;
    String type;
}
