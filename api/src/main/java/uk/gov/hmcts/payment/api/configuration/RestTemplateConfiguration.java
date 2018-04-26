package uk.gov.hmcts.payment.api.configuration;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestTemplateConfiguration {
    @Bean(name = {"paymentsHttpClient", "serviceTokenParserHttpClient", "userTokenParserHttpClient"})
    public CloseableHttpClient paymentsHttpClient() {
        return HttpClients.custom()
            .useSystemProperties()
            .build();
    }
}
