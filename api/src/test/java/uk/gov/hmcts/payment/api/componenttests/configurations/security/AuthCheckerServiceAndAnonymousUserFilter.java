package uk.gov.hmcts.payment.api.componenttests.configurations.security;

import groovy.util.logging.Slf4j;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import uk.gov.hmcts.reform.auth.checker.core.RequestAuthorizer;
import uk.gov.hmcts.reform.auth.checker.core.exceptions.AuthCheckerException;
import uk.gov.hmcts.reform.auth.checker.core.exceptions.BearerTokenMissingException;
import uk.gov.hmcts.reform.auth.checker.core.service.Service;
import uk.gov.hmcts.reform.auth.checker.core.user.User;
import uk.gov.hmcts.reform.auth.checker.core.user.UserRequestAuthorizer;
import uk.gov.hmcts.reform.auth.checker.spring.serviceanduser.ServiceAndUserPair;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class AuthCheckerServiceAndAnonymousUserFilter extends AbstractPreAuthenticatedProcessingFilter {


    private final RequestAuthorizer<Service> serviceRequestAuthorizer;
    private final RequestAuthorizer<User> userRequestAuthorizer;
    private static final Set anonymousRole = new HashSet<String>(Arrays.asList("ROLE_ANONYMOUS"));

    public AuthCheckerServiceAndAnonymousUserFilter(RequestAuthorizer<Service> serviceRequestAuthorizer,
                                                    RequestAuthorizer<User> userRequestAuthorizer) {
        this.serviceRequestAuthorizer = serviceRequestAuthorizer;
        this.userRequestAuthorizer = userRequestAuthorizer;
    }


    @Override
    protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
        Service service = authorizeService(request);
        if (service == null) {
            return null;
        }
        User user = authorizeUser(request);

        if (user == null)
            return null;

        return new ServiceAndUserPair(service, user);
    }

    @Override
    protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
        String preAuthenticatedCredentials = request.getHeader(UserRequestAuthorizer.AUTHORISATION);
        return (preAuthenticatedCredentials != null) ? preAuthenticatedCredentials : " " ;
    }

    private User authorizeUser(HttpServletRequest request) {
        try {
            return userRequestAuthorizer.authorise(request);
        } catch (BearerTokenMissingException btme) {
            return new User("anonymous", anonymousRole);
        } catch(AuthCheckerException ace) {
//            log.debug("Unsuccessful user authentication", ace);
            return null;
        }
    }

    private Service authorizeService(HttpServletRequest request) {
        try {
            return serviceRequestAuthorizer.authorise(request);
        } catch (AuthCheckerException e) {
//            log.warn("Unsuccessful service authentication"+ e.toString());
            return null;
        }
    }


}
