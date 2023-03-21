package uk.gov.hmcts.payment.api.domain.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder(builderMethodName = "rolesWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Roles {

   private List<String> roles;
}
