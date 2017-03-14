package uk.gov.hmcts.payment.api.configuration.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.model.UserIdSupplier;

@Component
public class AuthenticatedUserIdSupplier implements UserIdSupplier {
    @Override
    public String get() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return user.getUsername();
    }
}
