package uk.gov.hmcts.payment.api.configuration.security;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SpringSecurityConfiguration extends WebSecurityConfigurerAdapter {

    private ServiceAuthFilter serviceAuthFilter;
    private List<String> authorisedServices = new ArrayList<>();


        @Autowired
        public SpringSecurityConfiguration(final AuthTokenValidator authTokenValidator) {
            authorisedServices.add("ccpay-bubble");
            serviceAuthFilter = new ServiceAuthFilter(authTokenValidator, authorisedServices);

        }

        protected void configure(HttpSecurity http) throws Exception {
            http
                .requestMatchers()
                    .antMatchers(HttpMethod.GET, "/payments")
                    .antMatchers(HttpMethod.GET, "/payments1")
                    .antMatchers(HttpMethod.PATCH, "/payments/**")
                    .antMatchers(HttpMethod.POST, "/telephony/callback")
                    .antMatchers(  "/jobs/**")
                    .and()
                .addFilterBefore(serviceAuthFilter, BearerTokenAuthenticationFilter.class)
                .csrf().disable()
                .authorizeRequests()
                .anyRequest().authenticated();
        }
    }
