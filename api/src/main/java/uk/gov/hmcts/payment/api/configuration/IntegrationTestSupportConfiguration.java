package uk.gov.hmcts.payment.api.configuration;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@Configuration
@ConditionalOnProperty("payment.testing-support.enabled")
public class IntegrationTestSupportConfiguration {
    private final WireMockServer wireMockServer;

    public IntegrationTestSupportConfiguration(@Value("${payment.testing-support.wiremock.port}") int port) {
        this.wireMockServer = new WireMockServer(wireMockConfig().port(port));
        this.wireMockServer.start();
    }
}
