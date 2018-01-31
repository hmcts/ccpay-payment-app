package uk.gov.hmcts.payment.api.configuration.security;


import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import uk.gov.hmcts.auth.checker.spring.serviceanduser.AuthCheckerServiceAndUserFilter;
import uk.gov.hmcts.auth.checker.spring.serviceonly.AuthCheckerServiceOnlyFilter;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;


@EnableWebSecurity
public class  SpringSecurityConfiguration {

    @Order(1)
    @Configuration
    public class ServiceOnlySecurityConfiguration extends WebSecurityConfigurerAdapter {

        @Autowired
        private AuthCheckerServiceOnlyFilter serviceAuthCheckerFilter;

        @Override
        @SuppressWarnings(value = "SPRING_CSRF_PROTECTION_DISABLED", justification = "It's safe to disable CSRF protection as application is not being hit directly from the browser")
        protected void configure(HttpSecurity http) throws Exception {
            serviceAuthCheckerFilter.setAuthenticationManager(authenticationManager());

            http.antMatcher("/payments/reconcile")
                .addFilter(serviceAuthCheckerFilter)
                .sessionManagement().sessionCreationPolicy(STATELESS).and()
                .csrf().disable()
                .formLogin().disable()
                .logout().disable()
                .authorizeRequests()
                .anyRequest().authenticated();
        }
    }

    @Configuration
    public class ServiceAndUserSecurityConfiguration extends WebSecurityConfigurerAdapter {

        @Autowired
        private AuthCheckerServiceAndUserFilter serviceAndUserAuthCheckerFilter;

        @Override
        @SuppressWarnings(value = "SPRING_CSRF_PROTECTION_DISABLED", justification = "It's safe to disable CSRF protection as application is not being hit directly from the browser")
        protected void configure(HttpSecurity http) throws Exception {
            serviceAndUserAuthCheckerFilter.setAuthenticationManager(authenticationManager());

            http
                .addFilter(serviceAndUserAuthCheckerFilter)
                .sessionManagement().sessionCreationPolicy(STATELESS).and()
                .csrf().disable()
                .formLogin().disable()
                .logout().disable()
                .authorizeRequests()
                .antMatchers("/swagger-ui.html", "/webjars/springfox-swagger-ui/**", "/swagger-resources/**", "/v2/**", "/health", "/info", "/refdata/**").permitAll()
                .anyRequest().authenticated();
        }
    }


}
