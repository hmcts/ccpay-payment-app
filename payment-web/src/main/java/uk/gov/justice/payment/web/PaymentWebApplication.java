package uk.gov.justice.payment.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;

@SpringBootApplication
@EnableAutoConfiguration
@Controller
public class PaymentWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentWebApplication.class, args);
    }
}
