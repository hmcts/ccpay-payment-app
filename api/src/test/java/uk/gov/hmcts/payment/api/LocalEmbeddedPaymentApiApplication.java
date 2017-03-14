package uk.gov.hmcts.payment.api;

import org.springframework.boot.builder.SpringApplicationBuilder;


public class LocalEmbeddedPaymentApiApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder()
                .sources(PaymentApiApplication.class)
                .profiles("local")
                .profiles("embedded")
                .run();
    }
}
