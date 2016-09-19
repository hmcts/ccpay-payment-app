package uk.gov.justice.payment.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

@Configuration
@PropertySource("classpath:payment.properties")

public class PaymentConfig {

    @Autowired
    Environment environment;
}
