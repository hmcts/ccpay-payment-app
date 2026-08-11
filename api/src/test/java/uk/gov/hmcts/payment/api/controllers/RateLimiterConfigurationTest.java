package uk.gov.hmcts.payment.api.controllers;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.springboot3.ratelimiter.autoconfigure.RateLimiterAutoConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RateLimiterConfigurationTest.TestConfig.class)
@TestPropertySource(properties = {
    "resilience4j.ratelimiter.instances.defaultRateLimiter.limitForPeriod=1",
    "resilience4j.ratelimiter.instances.defaultRateLimiter.limitRefreshPeriod=1m",
    "resilience4j.ratelimiter.instances.defaultRateLimiter.timeoutDuration=0"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RateLimiterConfigurationTest {

    @Autowired
    private RateLimitedComponent rateLimitedComponent;

    @Test
    public void shouldRejectCallsOverConfiguredRateLimit() {
        assertEquals("ok", rateLimitedComponent.call());

        assertThrows(RequestNotPermitted.class, () -> rateLimitedComponent.call());
    }

    @Configuration
    @ImportAutoConfiguration({
        AopAutoConfiguration.class,
        RateLimiterAutoConfiguration.class
    })
    static class TestConfig {
        @Bean
        RateLimitedComponent rateLimitedComponent() {
            return new RateLimitedComponent();
        }
    }

    @RateLimiter(name = "defaultRateLimiter")
    static class RateLimitedComponent {
        String call() {
            return "ok";
        }
    }
}
