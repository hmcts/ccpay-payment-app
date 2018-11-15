package uk.gov.hmcts.payment.api.configuration;

import org.ff4j.FF4j;
import org.ff4j.core.Feature;
import org.ff4j.web.ApiConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.payment.api.service.CallbackService;

@Configuration
@ConditionalOnClass({FF4j.class})
@ComponentScan(value = {"org.ff4j.spring.boot.web.api", "org.ff4j.services", "org.ff4j.aop", "org.ff4j.spring"})
public class FF4jConfiguration {

    @Value("${feature.payments.search}")
    private boolean paymentSearch = false;

    @Value("${feature.payments.service.callback}")
    private boolean paymentServiceCallback = false;

    @Bean
    public FF4j getFf4j() {

        Feature paymentSearchFeature = new Feature(
            "payment-search",
            paymentSearch,
            "Payments search API"
        );

        Feature paymentServiceCallbackFeature = new Feature(
            CallbackService.FEATURE,
            paymentServiceCallback,
            "Payment Service Callback"
        );

        FF4j ff4j = new FF4j()
            .createFeature(paymentSearchFeature)
            .createFeature(paymentServiceCallbackFeature);

        return ff4j;
    }

    @Bean
    public ApiConfig getApiConfig() {
        ApiConfig apiConfig = new ApiConfig();

        //apiConfig.setAuthenticate(false);
        //apiConfig.createUser("admin", "password", true, true, Util.set("ADMIN", "USER"));
        //apiConfig.setAutorize(false);

        apiConfig.setWebContext("/api");
        apiConfig.setFF4j(getFf4j());
        return apiConfig;
    }
}
