package uk.gov.hmcts.payment.api.configuration.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;

import java.util.UUID;

@Component
public class AuthenticatedUserIdSupplier implements UserIdSupplier {
    @Override
    public String get() {
        // DTRJ: Commented out to avoid role checking - DO NOT PUSH TO MASTER
        //User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        //return user.getUsername();

        //return UUID.randomUUID().toString();

        // // DTRJ: Hard coded user id - DO NOT PUSH TO MASTER
        return "128084c2-504f-4e8a-958a-c72d0036eee7";
    }
}
