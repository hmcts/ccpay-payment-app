package uk.gov.hmcts.payment.api.v1.componenttests.backdoors;

import uk.gov.hmcts.reform.auth.checker.core.SubjectResolver;
import uk.gov.hmcts.reform.auth.checker.core.user.User;

public interface UserResolverBackdoor extends SubjectResolver<User> {
    void registerToken(String token, String userId);
}
