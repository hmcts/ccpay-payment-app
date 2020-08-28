package uk.gov.hmcts.payment.api.jpaaudit.listner;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
//@EnableJpaRepositories("uk.gov.hmcts.payment.api.jpaaudit.listner")
class AuditConfiguration {
    @Bean
    public AuditorAware<String> auditorAware() {
        return new EntityAuditorAware();
    }
}
