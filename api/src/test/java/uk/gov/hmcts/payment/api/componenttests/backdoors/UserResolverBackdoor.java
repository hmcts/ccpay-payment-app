package uk.gov.hmcts.payment.api.componenttests.backdoors;

import com.google.common.collect.ImmutableSet;
import java.util.concurrent.ConcurrentHashMap;
import uk.gov.hmcts.auth.checker.SubjectResolver;
import uk.gov.hmcts.auth.checker.user.User;
import uk.gov.hmcts.auth.checker.exceptions.AuthCheckerException;

public class UserResolverBackdoor implements SubjectResolver<User> {
    private final ConcurrentHashMap<String, String> tokenToUserMap = new ConcurrentHashMap<>();

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
