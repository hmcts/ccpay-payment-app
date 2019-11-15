package uk.gov.hmcts.payment.api.configuration.security;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.reform.auth.checker.spring.serviceanduser.ServiceAndUserDetails;
import uk.gov.hmcts.reform.auth.checker.spring.serviceonly.ServiceDetails;

import java.util.Optional;

@Component
public class AuthenticatedServiceIdSupplier implements ServiceIdSupplier {
    private static final Logger LOG = LoggerFactory.getLogger(AuthenticatedServiceIdSupplier.class);
    @Override
    public String get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logAuthenticationObj(authentication);
        return Optional.ofNullable(authentication)
            .map(a -> getServicename(a))
            .orElse(null);
    }

    private String logAuthenticationObj(Authentication authentication) {
        Gson gson = new GsonBuilder().
            serializeSpecialFloatingPointValues().serializeNulls().create();
        LOG.info(gson.toJson(authentication));

        return null;
    }
    private String getServicename(Authentication authentication) {
        if (authentication.getPrincipal() instanceof ServiceAndUserDetails) {
            LOG.info("Service name : {}",((ServiceAndUserDetails) authentication.getPrincipal()).getServicename());
            return ((ServiceAndUserDetails) authentication.getPrincipal()).getServicename();
        }
        return ((ServiceDetails) authentication.getPrincipal()).getUsername();
    }
}
