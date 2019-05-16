package uk.gov.hmcts.payment.integration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ComponentScan("uk.gov.hmcts.payment.integration")
@PropertySource("classpath:application-integration-tests.properties")
public class IntegrationTestContextConfiguration {
}
