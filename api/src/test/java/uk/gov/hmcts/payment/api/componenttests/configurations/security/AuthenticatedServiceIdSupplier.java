package uk.gov.hmcts.payment.api.componenttests.configurations.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.reform.auth.checker.spring.serviceanduser.ServiceAndUserDetails;
import uk.gov.hmcts.reform.auth.checker.spring.serviceonly.ServiceDetails;

import java.util.Optional;

@Component
public class AuthenticatedServiceIdSupplier implements ServiceIdSupplier {
    @Override
    public String get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Optional.ofNullable(authentication)
            .map(a -> getServicename(a))
            .orElse(null);
    }

    private String getServicename(Authentication authentication) {
        if (authentication.getPrincipal() instanceof ServiceAndUserDetails) {
            return ((ServiceAndUserDetails) authentication.getPrincipal()).getServicename();
        }
        return ((ServiceDetails) authentication.getPrincipal()).getUsername();
    }
}
