package uk.gov.hmcts.payment.api.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import uk.gov.hmcts.payment.api.service.AccountService;
import uk.gov.hmcts.payment.api.service.MockAccountServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;

class LiberataStubAccountConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(LiberataStubAccountConfiguration.class);

    @Test
    void shouldCreateStubWhenEnabled() {
        contextRunner
            .withPropertyValues("liberata.api.realtime.stub.enabled=true")
            .run(context -> {
                assertThat(context).hasSingleBean(AccountService.class);
                assertThat(context.getBean(AccountService.class)).isInstanceOf(MockAccountServiceImpl.class);
            });
    }

    @Test
    void shouldNotCreateStubWhenDisabled() {
        contextRunner
            .withPropertyValues("liberata.api.realtime.stub.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(AccountService.class));
    }

    @Test
    void shouldNotCreateStubByDefault() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(AccountService.class));
    }
}
