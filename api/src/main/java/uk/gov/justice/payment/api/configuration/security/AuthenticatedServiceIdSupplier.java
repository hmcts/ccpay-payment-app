package uk.gov.justice.payment.api.configuration.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.auth.checker.spring.serviceanduser.ServiceAndUserDetails;
import uk.gov.justice.payment.api.model.ServiceIdSupplier;

@Component
public class AuthenticatedServiceIdSupplier implements ServiceIdSupplier {
    @Override
    public String get() {
        ServiceAndUserDetails serviceAndUser = (ServiceAndUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return serviceAndUser.getServicename();
    }

}
