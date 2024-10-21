package uk.gov.hmcts.payment.api.configuration;

import com.microsoft.applicationinsights.TelemetryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppInsightsConfiguration {

    @Bean
    public TelemetryClient telemetryClient() {
        return new TelemetryClient();
    }
}