
package uk.gov.hmcts.payment.api.external.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderMethodName = "linkWith")
public class Link {
    private String type;
    private Map params;
    private String href;
    private String method;
}
