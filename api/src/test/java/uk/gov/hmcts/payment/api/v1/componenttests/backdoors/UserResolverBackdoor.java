package uk.gov.hmcts.payment.api.v1.componenttests.backdoors;


import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.testcontainers.shaded.com.google.common.collect.ImmutableSet;
import uk.gov.hmcts.reform.auth.checker.core.SubjectResolver;
import uk.gov.hmcts.reform.auth.checker.core.exceptions.AuthCheckerException;
import uk.gov.hmcts.reform.auth.checker.core.user.User;

import java.util.concurrent.ConcurrentHashMap;

@Component
@Primary
public class UserResolverBackdoor implements SubjectResolver<User>{
    private final ConcurrentHashMap<String, User> tokenToUserMap = new ConcurrentHashMap<>();

    public final static String CITIZEN_ID = "1";
    public final static String CASEWORKER_ID = "2";
    public final static String AUTHENTICATED_USER_ID = "3";
    public final static String SOLICITOR_ID = "4";

    public UserResolverBackdoor() {

        tokenToUserMap.put("Bearer user-1", new User(CITIZEN_ID, ImmutableSet.of("citizen")));
        tokenToUserMap.put("Bearer caseworker-2", new User(CASEWORKER_ID, ImmutableSet.of("caseworker", "payments")));
        tokenToUserMap.put("Bearer authenticated-3", new User(AUTHENTICATED_USER_ID, ImmutableSet.of()));
        tokenToUserMap.put("Bearer solicitor-4", new User(SOLICITOR_ID, ImmutableSet.of("caseworker", "caseworker-probate-solicitor")));
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
