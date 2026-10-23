package uk.gov.hmcts.payment.api.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.service.AccountService;
import uk.gov.hmcts.payment.api.service.MockAccountServiceImpl;

@Configuration
@Profile("!liberataMock")
@ConditionalOnProperty(name = "liberata.api.realtime.stub.enabled", havingValue = "true")
public class LiberataStubAccountConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(LiberataStubAccountConfiguration.class);

    @Bean
    public AccountService<AccountDto, String> liberataStubAccountService() {
        LOG.warn("Liberata realtime account stub is enabled. No calls will be made to Liberata.");
        return new MockAccountServiceImpl();
    }
}
