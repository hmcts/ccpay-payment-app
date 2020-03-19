package uk.gov.hmcts.payment.api.configuration.security;


import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.config.http.SessionCreationPolicy;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import org.springframework.security.oauth2.jwt.*;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@EnableWebSecurity
public class SpringSecurityConfiguration {


    @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}")
    private String issuerUri;

    @Value("${oidc.issuer}")
    private String issuerOverride;

    @Configuration
    @Order(1)
    public static class ExternalApiSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        private ServiceAuthFilter serviceAuthFilter;


        @Autowired
        public ExternalApiSecurityConfigurationAdapter(final ServiceAuthFilter serviceAuthFilter) {
            this.serviceAuthFilter = serviceAuthFilter;
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
                .csrf().disable()
                .formLogin().disable()
                .logout().disable()
                .addFilterBefore(serviceAuthFilter, BearerTokenAuthenticationFilter.class)
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        }
    }

    @Configuration
    @Order(2)
    public static class InternalApiSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        private AuthCheckerServiceAndAnonymousUserFilter authCheckerFilter;

        @Autowired
        public InternalApiSecurityConfigurationAdapter(final ServiceAuthFilter serviceAuthFilter) {
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
                .antMatchers(HttpMethod.GET, "/card-payments/*/details").hasAnyAuthority("payments", "citizen")
                .antMatchers(HttpMethod.GET, "/pba-accounts/*/payments").hasAnyAuthority("payments","pui-finance-manager","caseworker-cmc-solicitor", "caseworker-publiclaw-solicitor", "caseworker-probate-solicitor", "caseworker-financialremedy-solicitor", "caseworker-divorce-solicitor")
                .antMatchers(HttpMethod.GET, "/card-payments/*/status").hasAnyAuthority("payments", "citizen")
                .antMatchers(HttpMethod.GET, "/reference-data/**").permitAll()
                .antMatchers(HttpMethod.POST, "/api/**").permitAll()
                .anyRequest().authenticated();
        }
    }

    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder)JwtDecoders.fromOidcIssuerLocation(issuerUri);

        // We are using issuerOverride instead of issuerUri as SIDAM has the wrong issuer at the moment
        OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();
        OAuth2TokenValidator<Jwt> withIssuer = new JwtIssuerValidator(issuerOverride);
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withTimestamp, withIssuer);

        jwtDecoder.setJwtValidator(validator);
        return jwtDecoder;
    }

}
