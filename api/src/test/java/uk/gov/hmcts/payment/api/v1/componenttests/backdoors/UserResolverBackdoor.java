package uk.gov.hmcts.payment.api.v1.componenttests.backdoors;

import com.google.common.collect.ImmutableSet;
import uk.gov.hmcts.reform.auth.checker.core.SubjectResolver;
import uk.gov.hmcts.reform.auth.checker.core.exceptions.AuthCheckerException;
import uk.gov.hmcts.reform.auth.checker.core.user.User;

import java.util.concurrent.ConcurrentHashMap;

public class UserResolverBackdoor implements SubjectResolver<User> {
    private final ConcurrentHashMap<String, String> tokenToUserMap = new ConcurrentHashMap<>();

    public UserResolverBackdoor() {
        tokenToUserMap.put("Bearer user-1", "1");
    }

    @Override
    public User getTokenDetails(String token) {
        String userId = tokenToUserMap.get(token);

        if (userId == null) {
            throw new AuthCheckerException("Token not found");
        }

        return new User(userId, ImmutableSet.of("citizen"));
    }

    public void registerToken(String token, String userId) {
        tokenToUserMap.put(token, userId);
    }
}
