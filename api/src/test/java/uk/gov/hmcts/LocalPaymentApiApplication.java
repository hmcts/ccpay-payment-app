package uk.gov.hmcts;

import org.springframework.boot.builder.SpringApplicationBuilder;

public class LocalPaymentApiApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder()
            .sources(PaymentApiApplication.class)
            .profiles("local")
            .run();
    }
}
