package uk.gov.hmcts.payment.api.configuration;

import com.launchdarkly.sdk.server.Components;
import com.launchdarkly.sdk.server.LDClient;
import com.launchdarkly.sdk.server.LDConfig;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class FeatureToggleConfiguration {

    @Value("${launchDarkly.sdkKey}")
    private String sdkKey;

    @Value("${launchDarkly.connectionTimeout}")
    private Integer connectionTimeout;

    @Value("${launchDarkly.socketTimeout}")
    private Integer socketTimeout;

    @Value("${launchDarkly.flushInterval}")
    private Integer flushInterval;

    @Bean
    public LDConfig ldConfig() {
        return new LDConfig.Builder()
            .http(
                Components.httpConfiguration()
                    .connectTimeout(Duration.ofSeconds(connectionTimeout))
                    .socketTimeout(Duration.ofSeconds(socketTimeout))
            )
            .events(
                Components.sendEvents()
                    .flushInterval(Duration.ofSeconds(flushInterval))
            )
            .build();
    }

    @Bean
    public LDClientInterface ldClient(LDConfig ldConfig) {
        return new LDClient(sdkKey, ldConfig);
    }

}
