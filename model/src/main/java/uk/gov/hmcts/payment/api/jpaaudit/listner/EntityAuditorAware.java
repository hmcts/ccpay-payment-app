package uk.gov.hmcts.payment.api.jpaaudit.listner;

import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

public class EntityAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.of("PAYMENT-USER");
    }
}
