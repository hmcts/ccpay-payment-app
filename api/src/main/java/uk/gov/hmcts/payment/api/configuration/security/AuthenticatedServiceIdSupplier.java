package uk.gov.hmcts.payment.api.configuration.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.reform.auth.checker.spring.serviceanduser.ServiceAndUserDetails;

import java.util.Optional;

@Component
public class AuthenticatedServiceIdSupplier implements ServiceIdSupplier {
    @Override
    public String get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Optional.ofNullable(authentication)
            .map(a -> ((ServiceAndUserDetails) a.getPrincipal()).getServicename())
            .orElse(null);
    }
}
