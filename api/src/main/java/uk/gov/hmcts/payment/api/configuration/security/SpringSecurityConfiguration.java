package uk.gov.hmcts.payment.api.configuration.security;


import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;

import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;


@EnableWebSecurity
public class SpringSecurityConfiguration extends WebSecurityConfigurerAdapter{

    private final ServiceAuthFilter serviceAuthFilter;

    public SpringSecurityConfiguration(final ServiceAuthFilter serviceAuthFilter) {
        this.serviceAuthFilter = serviceAuthFilter;
    }

    @Override
    public void configure(WebSecurity web) {
        web.ignoring().antMatchers("/swagger-ui.html",
            "/webjars/springfox-swagger-ui/**",
            "/swagger-resources/**",
            "/v2/**",
            "/refdata/**",
            "/health",
            "/health/liveness",
            "/info",
            "/favicon.ico",
            "/mock-api/**",
            "/");
    }

    @Override
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
            .formLogin().disable()
            .logout().disable()
            .authorizeRequests()
            .anyRequest().authenticated();
    }
}
