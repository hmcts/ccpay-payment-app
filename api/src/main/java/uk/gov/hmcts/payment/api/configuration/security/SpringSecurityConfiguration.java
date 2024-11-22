package uk.gov.hmcts.payment.api.configuration.security;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import uk.gov.hmcts.reform.auth.checker.core.RequestAuthorizer;
import uk.gov.hmcts.reform.auth.checker.core.service.Service;
import uk.gov.hmcts.reform.auth.checker.core.user.User;
import uk.gov.hmcts.reform.auth.checker.spring.serviceonly.AuthCheckerServiceOnlyFilter;


import jakarta.servlet.http.HttpServletResponse;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
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
    private final AuthCheckerServiceOnlyFilter authCheckerServiceOnlyFilter;
    private final AuthCheckerServiceAndAnonymousUserFilter authCheckerUserOnlyFilter;

    public SpringSecurityConfiguration(
        RequestAuthorizer<Service> serviceRequestAuthorizer,
        AuthenticationManager authenticationManager, RequestAuthorizer<User> userRequestAuthorizer
    ) {
        authCheckerServiceOnlyFilter = new AuthCheckerServiceOnlyFilter(serviceRequestAuthorizer);
        authCheckerServiceOnlyFilter.setAuthenticationManager(authenticationManager);
        authCheckerUserOnlyFilter = new AuthCheckerServiceAndAnonymousUserFilter(serviceRequestAuthorizer, userRequestAuthorizer);
        authCheckerUserOnlyFilter.setAuthenticationManager(authenticationManager);
    }

    @Bean
    @Order(3)
    protected SecurityFilterChain configureExternal(HttpSecurity http) throws Exception {
        http
            .addFilter(authCheckerServiceOnlyFilter)
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.GET, "/payments").authenticated()
                .requestMatchers(HttpMethod.GET, "/payments1").authenticated()
                .requestMatchers(HttpMethod.GET, "/card-payments/*/status").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/payments/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/telephony/callback").authenticated()
                .requestMatchers("/jobs/**").authenticated()
                .anyRequest().authenticated()
            );
        return http.build();
    }

    @Bean
    @Order(2)
    protected SecurityFilterChain configureInternal(HttpSecurity http) throws Exception {
        http.exceptionHandling(exceptionHandling -> exceptionHandling.accessDeniedHandler(accessDeniedHandler()))
            .addFilter(authCheckerUserOnlyFilter)
            .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(STATELESS))
            .csrf(AbstractHttpConfigurer::disable) //NOSONAR
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.GET, "/cases/{ccdcasenumber}/paymentgroups").hasAnyAuthority(PAYMENTS_ROLE, CASE_MANAGER_ROLE, FINANCE_MANAGER_ROLE, ORGANISATION_MANAGER_ROLE, USER_MANAGER_ROLE)
                .requestMatchers(HttpMethod.GET, "/cases/{case}/payments").hasAuthority(PAYMENTS_ROLE)
                .requestMatchers(HttpMethod.DELETE, "/fees/**").hasAuthority(PAYMENTS_ROLE)
                .requestMatchers(HttpMethod.POST, "/card-payments").hasAnyAuthority(PAYMENTS_ROLE, CITIZEN_ROLE)
                .requestMatchers(HttpMethod.POST, "/card-payments/*/cancel").hasAnyAuthority(PAYMENTS_ROLE, CITIZEN_ROLE)
                .requestMatchers(HttpMethod.GET, "/card-payments/*/details").hasAnyAuthority(PAYMENTS_ROLE, CITIZEN_ROLE)
                .requestMatchers(HttpMethod.GET, "/pba-accounts/*/payments").hasAnyAuthority(PAYMENTS_ROLE,"pui-finance-manager","caseworker-cmc-solicitor", "caseworker-publiclaw-solicitor", "caseworker-probate-solicitor", "caseworker-financialremedy-solicitor", "caseworker-divorce-solicitor")
                //.requestMatchers(HttpMethod.GET, "/card-payments/*/status").hasAnyAuthority(PAYMENTS_ROLE, CITIZEN_ROLE)
                .requestMatchers(HttpMethod.POST,"/refund-for-payment").hasAnyAuthority(AUTHORISED_REFUNDS_APPROVER_ROLE,AUTHORISED_REFUNDS_ROLE)
                .requestMatchers(HttpMethod.POST,"/refund-retro-remission").hasAnyAuthority(AUTHORISED_REFUNDS_APPROVER_ROLE,AUTHORISED_REFUNDS_ROLE)
                .requestMatchers(HttpMethod.PATCH,"/refund/resubmit/*").hasAnyAuthority(AUTHORISED_REFUNDS_APPROVER_ROLE,AUTHORISED_REFUNDS_ROLE)
                .requestMatchers(HttpMethod.GET, "/reference-data/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/case-payment-orders**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/payment-failures/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/payment-failures/**").hasAuthority(PAYMENTS_ROLE)
                .requestMatchers(HttpMethod.PATCH, "/payment-failures/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }

    @Bean
    @Order(1)
    protected SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(
                "/swagger-ui.html",
                "/swagger-resources/**",
                "/swagger-ui/**",
                "/v3/**",
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
                "/")
            .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
            .anonymous(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable);
        return http.build();
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
}
