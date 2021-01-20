package uk.gov.hmcts.payment.api.configuration.security;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import uk.gov.hmcts.payment.api.configuration.SecurityUtils;
import uk.gov.hmcts.payment.api.configuration.converters.JwtGrantedAuthoritiesConverter;
import uk.gov.hmcts.payment.api.configuration.validator.AudienceValidator;
import uk.gov.hmcts.reform.auth.checker.core.RequestAuthorizer;
import uk.gov.hmcts.reform.auth.checker.core.service.Service;
import uk.gov.hmcts.reform.auth.checker.core.user.User;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@EnableWebSecurity
public class SpringSecurityConfiguration {

    private static final String CITIZEN_ROLE = "citizen";
    private static final String PAYMENTS_ROLE = "payments";

    @Configuration
    @Order(1)
    public static class ExternalApiSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        private ServiceAuthFilter serviceAuthFilter;


        private ServicePaymentFilter servicePaymentFilter;


        @Inject
        public ExternalApiSecurityConfigurationAdapter(final ServiceAuthFilter serviceAuthFilter,final ServicePaymentFilter servicePaymentFilter) {
            super();
            this.serviceAuthFilter =  serviceAuthFilter;
            this.servicePaymentFilter = servicePaymentFilter;
        }

        protected void configure(HttpSecurity http) throws Exception {
                http
                    .addFilterBefore(serviceAuthFilter, BearerTokenAuthenticationFilter.class)
                    .addFilterAfter(servicePaymentFilter,ServiceAuthFilter.class)
                    .sessionManagement().sessionCreationPolicy(STATELESS).and().anonymous().disable()
                    .csrf().disable()
                    .formLogin().disable()
                    .logout().disable()
                    .requestMatchers()
                    .antMatchers(HttpMethod.GET, "/payments")
                    .antMatchers(HttpMethod.GET, "/payments1")
                    .antMatchers(HttpMethod.PATCH, "/payments/**")
                    .antMatchers(HttpMethod.POST, "/telephony/callback")
                    .antMatchers( "/jobs/**");
        }

    }

    @Configuration
    @Order(2)
    public static class InternalApiSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        private ServiceAuthFilter serviceAuthFilter;

        private ServiceAndUserAuthFilter serviceAndUserAuthFilter;

        private JwtAuthenticationConverter jwtAuthenticationConverter;

        private ServicePaymentFilter servicePaymentFilter;


        @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}")
        private String issuerUri;

        @Value("${oidc.audience-list}")
        private String[] allowedAudiences;

        @Value("${oidc.issuer}")
        private String issuerOverride;

        private AuthCheckerServiceAndAnonymousUserFilter authCheckerFilter;


        @Inject
        public InternalApiSecurityConfigurationAdapter(final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter, final ServiceAuthFilter serviceAuthFilter, final Function<HttpServletRequest, Optional<String>> userIdExtractor,
                                                       final Function<HttpServletRequest, Collection<String>> authorizedRolesExtractor,
                                                       final SecurityUtils securityUtils, final ServicePaymentFilter servicePaymentFilter,
                                                       RequestAuthorizer<User> userRequestAuthorizer,
                                                       RequestAuthorizer<Service> serviceRequestAuthorizer, AuthenticationManager authenticationManager) {
            super();
            this.serviceAuthFilter =  serviceAuthFilter;
            this.servicePaymentFilter = servicePaymentFilter;
            this.serviceAndUserAuthFilter = new ServiceAndUserAuthFilter(
                userIdExtractor, authorizedRolesExtractor, securityUtils);
            jwtAuthenticationConverter = new JwtAuthenticationConverter();
            jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
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
                "/",
                "/api/ff4j/**");
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
                .antMatchers(HttpMethod.GET, "/cases/**").hasAuthority(PAYMENTS_ROLE)
                .antMatchers(HttpMethod.DELETE, "/fees/**").hasAuthority(PAYMENTS_ROLE)
                .antMatchers(HttpMethod.POST, "/card-payments").hasAnyAuthority(PAYMENTS_ROLE, CITIZEN_ROLE)
                .antMatchers(HttpMethod.POST, "/card-payments/*/cancel").hasAnyAuthority(PAYMENTS_ROLE, CITIZEN_ROLE)
                .antMatchers(HttpMethod.GET, "/card-payments/*/details").hasAnyAuthority(PAYMENTS_ROLE, CITIZEN_ROLE)
                .antMatchers(HttpMethod.GET, "/pba-accounts/*/payments").hasAnyAuthority(PAYMENTS_ROLE,"pui-finance-manager","caseworker-cmc-solicitor", "caseworker-publiclaw-solicitor", "caseworker-probate-solicitor", "caseworker-financialremedy-solicitor", "caseworker-divorce-solicitor")
                .antMatchers(HttpMethod.GET, "/card-payments/*/status").hasAnyAuthority(PAYMENTS_ROLE, CITIZEN_ROLE)
                .antMatchers(HttpMethod.GET, "/reference-data/**").permitAll()
                .antMatchers(HttpMethod.POST, "/api/**").permitAll()
                .anyRequest().authenticated();
        }
    }

}
