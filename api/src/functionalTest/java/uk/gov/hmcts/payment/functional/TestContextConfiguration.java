package uk.gov.hmcts.payment.functional;


import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ComponentScan("uk.gov.hmcts.payment.functional")
@PropertySource("classpath:application-functional-tests.properties")
public class TestContextConfiguration {
}
