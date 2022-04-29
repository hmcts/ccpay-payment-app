package uk.gov.hmcts;

import org.springframework.boot.builder.SpringApplicationBuilder;
import uk.gov.hmcts.PaymentApiApplication;


public class LocalEmbeddedPaymentApiApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder()
                .sources(PaymentApiApplication.class)
                .profiles("local")
                .run();
        
    }
}
