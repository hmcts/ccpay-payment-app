package uk.gov.hmcts.payment.api.configuration.security;


import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import uk.gov.hmcts.reform.auth.checker.core.RequestAuthorizer;
import uk.gov.hmcts.reform.auth.checker.core.service.Service;
import uk.gov.hmcts.reform.auth.checker.core.user.User;
import uk.gov.hmcts.reform.auth.checker.spring.serviceonly.AuthCheckerServiceOnlyFilter;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@EnableWebSecurity
public class SpringSecurityConfiguration {

    @Configuration
    @Order(1)
    public static class ExternalApiSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        private AuthCheckerServiceOnlyFilter authCheckerServiceOnlyFilter;

        @Autowired
        public ExternalApiSecurityConfigurationAdapter(RequestAuthorizer<Service> serviceRequestAuthorizer,
                                           AuthenticationManager authenticationManager) {
            authCheckerServiceOnlyFilter = new AuthCheckerServiceOnlyFilter(serviceRequestAuthorizer);
            authCheckerServiceOnlyFilter.setAuthenticationManager(authenticationManager);
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
                .addFilter(authCheckerServiceOnlyFilter)
                .csrf().disable()
                .authorizeRequests()
                .anyRequest().authenticated();
        }
    }

    @Configuration
    @Order(2)
    public static class InternalApiSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        private AuthCheckerServiceAndAnonymousUserFilter authCheckerFilter;

        @Autowired
        public InternalApiSecurityConfigurationAdapter(RequestAuthorizer<User> userRequestAuthorizer,
                                           RequestAuthorizer<Service> serviceRequestAuthorizer,
                                           AuthenticationManager authenticationManager) {
            authCheckerFilter = new AuthCheckerServiceAndAnonymousUserFilter(serviceRequestAuthorizer, userRequestAuthorizer);
            authCheckerFilter.setAuthenticationManager(authenticationManager);
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
        @SuppressWarnings(value = "SPRING_CSRF_PROTECTION_DISABLED", justification = "It's safe to disable CSRF protection as application is not being hit directly from the browser")
        protected void configure(HttpSecurity http) throws Exception {
            http.addFilter(authCheckerFilter)
                .sessionManagement().sessionCreationPolicy(STATELESS).and()
                .csrf().disable()
                .formLogin().disable()
                .logout().disable()
                .authorizeRequests()
                .antMatchers(HttpMethod.GET, "/cases/**").hasAuthority("payments")
                .antMatchers(HttpMethod.DELETE, "/fees/**").hasAuthority("payments")
                .antMatchers(HttpMethod.POST, "/card-payments").hasAnyAuthority("payments", "citizen")
                .antMatchers(HttpMethod.POST, "/card-payments/*/cancel").hasAnyAuthority("payments", "citizen")
                .antMatchers(HttpMethod.GET, "/card-payments/*/details").hasAnyAuthority("payments", "citizen")
                .antMatchers(HttpMethod.GET, "/card-payments/*/statuses").hasAnyAuthority("payments", "citizen")
                .antMatchers(HttpMethod.GET, "/pba-accounts/*/payments").hasAnyAuthority("payments","pui-finance-manager","caseworker-cmc-solicitor", "caseworker-publiclaw-solicitor", "caseworker-probate-solicitor", "caseworker-financialremedy-solicitor", "caseworker-divorce-solicitor")
                .antMatchers(HttpMethod.GET, "/card-payments/*/status").hasAnyAuthority("payments", "citizen")
                .antMatchers(HttpMethod.GET, "/reference-data/**").permitAll()
                .antMatchers(HttpMethod.POST, "/api/**").permitAll()
                .anyRequest().authenticated();
        }
    }

}
