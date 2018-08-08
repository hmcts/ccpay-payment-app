package uk.gov.hmcts.payment.api.v1.componenttests.backdoors;

import com.google.common.collect.ImmutableSet;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.auth.checker.core.SubjectResolver;
import uk.gov.hmcts.reform.auth.checker.core.exceptions.AuthCheckerException;
import uk.gov.hmcts.reform.auth.checker.core.user.User;

import java.util.concurrent.ConcurrentHashMap;

@Component
@Primary
public class UserResolverBackdoor implements SubjectResolver<User>{
    private final ConcurrentHashMap<String, User> tokenToUserMap = new ConcurrentHashMap<>();

    public UserResolverBackdoor() {

        tokenToUserMap.put("Bearer user-1", new User("1", ImmutableSet.of("citizen")));
        tokenToUserMap.put("Bearer caseworker-2", new User("2", ImmutableSet.of("caseworker", "payments")));

    }

    @Override
    public User getTokenDetails(String token) {
        User user = tokenToUserMap.get(token);

        if (user == null) {
            throw new AuthCheckerException("Token not found");
        }

        return user;
    }

    public void registerToken(String token, String userId) {
        User user = tokenToUserMap.values().stream().filter(u -> u.getPrincipal().equals(userId)).findFirst().get();
        tokenToUserMap.put(token, user);
    }
}
