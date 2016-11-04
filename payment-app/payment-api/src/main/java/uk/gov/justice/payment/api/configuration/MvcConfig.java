package uk.gov.justice.payment.api.configuration;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import uk.gov.justice.payment.api.parameters.serviceid.ServiceIdHandlerMethodArgumentResolver;

import java.util.List;

@Configuration
public class MvcConfig extends WebMvcConfigurerAdapter {

    private final ServiceIdHandlerMethodArgumentResolver serviceIdHandlerMethodArgumentResolver;

    @Autowired
    public MvcConfig(ServiceIdHandlerMethodArgumentResolver serviceIdHandlerMethodArgumentResolver) {
        this.serviceIdHandlerMethodArgumentResolver = serviceIdHandlerMethodArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(serviceIdHandlerMethodArgumentResolver);
    }

}