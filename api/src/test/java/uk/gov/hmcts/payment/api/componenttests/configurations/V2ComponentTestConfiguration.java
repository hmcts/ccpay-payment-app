package uk.gov.hmcts.payment.api.componenttests.configurations;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.service.AccountService;
import uk.gov.hmcts.payment.referencedata.model.Site;
import uk.gov.hmcts.payment.referencedata.service.SiteService;

@Profile({"componenttest", "local"})
@Configuration
public class V2ComponentTestConfiguration {
    @Bean
    @Primary
    public AccountService<AccountDto, String> accountService() {
        return Mockito.mock(AccountService.class);
    }

    @Bean
    @Primary
    public SiteService<Site, String> siteService() {
        return Mockito.mock(SiteService.class);
    }
}
