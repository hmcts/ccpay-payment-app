package uk.gov.hmcts.payment.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder(builderMethodName = "userIdentityDataWith")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserIdentityDataDto {
    private String fullName;
    private String emailId;
}
