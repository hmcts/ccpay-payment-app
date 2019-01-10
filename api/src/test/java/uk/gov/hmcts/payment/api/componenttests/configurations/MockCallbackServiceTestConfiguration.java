package uk.gov.hmcts.payment.api.componenttests.configurations;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import uk.gov.hmcts.payment.api.servicebus.CallbackServiceImpl;

@Profile("mockcallbackservice")
@Configuration
public class MockCallbackServiceTestConfiguration {
    @Bean
    @Primary
    public CallbackServiceImpl nameService() {
        return Mockito.mock(CallbackServiceImpl.class);
    }
}
