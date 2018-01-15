package uk.gov.hmcts.payment.api.v1.componenttests;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.auth.checker.service.Service;
import uk.gov.hmcts.auth.checker.SubjectResolver;
import uk.gov.hmcts.auth.checker.user.User;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;

@Configuration
public class ComponentTestConfiguration {
    @Bean
    public SubjectResolver<Service> serviceResolver() {
        return new ServiceResolverBackdoor();
    }

    @Bean
    public SubjectResolver<User> userResolver() {
        return new UserResolverBackdoor();
    }

}
