package uk.gov.justice.payment.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

@Configuration
@PropertySource("classpath:payment.properties")

public class PaymentConfig {

    @Autowired
    Environment environment;

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }



}
