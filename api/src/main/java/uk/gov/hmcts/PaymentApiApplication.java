package uk.gov.hmcts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableFeignClients
@EnableAsync
@SpringBootApplication
public class PaymentApiApplication {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentApiApplication.class);

    public static void main(String[] args) {
        try {
            SpringApplication.run(PaymentApiApplication.class, args);
        } catch (Exception ex) {
            LOG.error("Application crashed with error message: ", ex);
        }
    }
}
