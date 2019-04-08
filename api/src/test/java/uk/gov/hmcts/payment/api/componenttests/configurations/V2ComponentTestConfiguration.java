package uk.gov.hmcts.payment.api.componenttests.configurations;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.service.AccountService;

@Profile({"componenttest", "local"})
@Configuration
public class V2ComponentTestConfiguration {
    @Bean
    @Primary
    public AccountService<AccountDto, String> nameService() {
        return Mockito.mock(AccountService.class);
    }
}
