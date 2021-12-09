package uk.gov.hmcts.payment.api.configuration.security;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.access.AccessDeniedHandler;
import uk.gov.hmcts.reform.auth.checker.core.RequestAuthorizer;
import uk.gov.hmcts.reform.auth.checker.core.service.Service;
import uk.gov.hmcts.reform.auth.checker.core.user.User;
import uk.gov.hmcts.reform.auth.checker.spring.serviceonly.AuthCheckerServiceOnlyFilter;

import javax.servlet.http.HttpServletResponse;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@EnableWebSecurity
public class SpringSecurityConfiguration {

    private static final String CITIZEN_ROLE = "citizen";
    private static final String PAYMENTS_ROLE = "payments";
    private static final String USER_MANAGER_ROLE = "pui-user-manager";
    private static final String ORGANISATION_MANAGER_ROLE = "pui-organisation-manager";
    private static final String FINANCE_MANAGER_ROLE = "pui-finance-manager";
    private static final String CASE_MANAGER_ROLE = "pui-case-manager";
    private static final String AUTHORISED_REFUNDS_ROLE = "payments-refund";
    private static final String AUTHORISED_REFUNDS_APPROVER_ROLE = "payments-refund-approver";

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

//        @Bean
//        public AccessDeniedHandler accessDeniedHandler() {
//            return (request, response, ex) -> {
//                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
//                response.setContentType(MediaType.TEXT_HTML_VALUE);
//                response.setStatus(403);
//                response.getWriter().print("Invalid Service Token");
//            };
//        }

        protected void configure(HttpSecurity http) throws Exception {
//            http.exceptionHandling().accessDeniedHandler(accessDeniedHandler());
            http
                .requestMatchers()
                    .antMatchers(HttpMethod.GET, "/payments")
                    .antMatchers(HttpMethod.GET, "/payments1")
                    .antMatchers(HttpMethod.PATCH, "/payments/**")
                    .antMatchers(HttpMethod.POST, "/telephony/callback")
                    .antMatchers(HttpMethod.GET, "/card-payments/*/status")
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
                "/refdata/methods",
                "/refdata/channels",
                "/refdata/providers",
                "/refdata/status",
                "/health",
                "/health/liveness",
                "/health/readiness",
                "/info",
                "/favicon.ico",
                "/mock-api/**",
                "/");
        }

        @Bean
        public AccessDeniedHandler accessDeniedHandler() {
            return (request, response, ex) -> {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.TEXT_HTML_VALUE);
                response.setStatus(403);
                response.getWriter().print("User does not have a valid role");
            };
        }


        @Override
        @SuppressWarnings(value = "SPRING_CSRF_PROTECTION_DISABLED", justification = "It's safe to disable CSRF protection as application is not being hit directly from the browser")
        protected void configure(HttpSecurity http) throws Exception {
            http.exceptionHandling().accessDeniedHandler(accessDeniedHandler());
            http.addFilter(authCheckerFilter)
                .sessionManagement().sessionCreationPolicy(STATELESS).and()
                .csrf().disable()
                .formLogin().disable()
                .logout().disable()
                .authorizeRequests()
                .antMatchers(HttpMethod.GET, "/cases/{ccdcasenumber}/paymentgroups").hasAnyAuthority(PAYMENTS_ROLE, CASE_MANAGER_ROLE, FINANCE_MANAGER_ROLE, ORGANISATION_MANAGER_ROLE, USER_MANAGER_ROLE)
                .antMatchers(HttpMethod.GET, "/cases/{case}/payments").hasAuthority(PAYMENTS_ROLE)
                .antMatchers(HttpMethod.DELETE, "/fees/**").hasAuthority(PAYMENTS_ROLE)
                .antMatchers(HttpMethod.POST, "/card-payments").hasAnyAuthority(PAYMENTS_ROLE, CITIZEN_ROLE)
                .antMatchers(HttpMethod.POST, "/card-payments/*/cancel").hasAnyAuthority(PAYMENTS_ROLE, CITIZEN_ROLE)
                .antMatchers(HttpMethod.GET, "/card-payments/*/details").hasAnyAuthority(PAYMENTS_ROLE, CITIZEN_ROLE)
                .antMatchers(HttpMethod.GET, "/pba-accounts/*/payments").hasAnyAuthority(PAYMENTS_ROLE,"pui-finance-manager","caseworker-cmc-solicitor", "caseworker-publiclaw-solicitor", "caseworker-probate-solicitor", "caseworker-financialremedy-solicitor", "caseworker-divorce-solicitor")
                .antMatchers(HttpMethod.GET, "/card-payments/*/status").hasAnyAuthority(PAYMENTS_ROLE, CITIZEN_ROLE)
                .antMatchers(HttpMethod.POST,"/refund-for-payment").hasAnyAuthority(AUTHORISED_REFUNDS_APPROVER_ROLE,AUTHORISED_REFUNDS_ROLE)
                .antMatchers(HttpMethod.POST,"/refund-retro-remission").hasAnyAuthority(AUTHORISED_REFUNDS_APPROVER_ROLE,AUTHORISED_REFUNDS_ROLE)
                .antMatchers(HttpMethod.PATCH,"/refund/resubmit/*").hasAnyAuthority(AUTHORISED_REFUNDS_APPROVER_ROLE,AUTHORISED_REFUNDS_ROLE)
                .antMatchers(HttpMethod.GET, "/reference-data/**").permitAll()
                .antMatchers(HttpMethod.GET, "/case-payment-orders**").permitAll()
                .antMatchers(HttpMethod.POST, "/api/**").permitAll()
                .anyRequest().authenticated();
        }
    }

}
