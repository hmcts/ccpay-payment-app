package uk.gov.hmcts.payment.api.configuration.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;
@Component
public class AuthenticatedUserIdSupplier implements UserIdSupplier {
    @Override
    public String get() {
        String userName = null;
        if(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken)
        {
            userName = ((JwtAuthenticationToken)SecurityContextHolder.getContext().getAuthentication()).getName();
        }
        else
        {
            User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            userName = user.getUsername();
        }
        return userName;
    }
}
