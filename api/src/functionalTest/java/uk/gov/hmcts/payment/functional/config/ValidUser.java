package uk.gov.hmcts.payment.functional.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder(builderMethodName = "userWith")
public class ValidUser {
    private final String email;
    private final String authorisationToken;
}
