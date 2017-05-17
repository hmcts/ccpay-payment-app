package uk.gov.hmcts.payment.api.configuration.security;


import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import uk.gov.hmcts.auth.checker.spring.serviceanduser.AuthCheckerServiceAndUserFilter;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
public class SpringSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    private AuthCheckerServiceAndUserFilter authCheckerFilter;

    @Override
    @SuppressWarnings(value = "SPRING_CSRF_PROTECTION_DISABLED", justification = "It's safe to disable CSRF protection as application is not being hit directly from the browser")
    protected void configure(HttpSecurity http) throws Exception {
        authCheckerFilter.setAuthenticationManager(authenticationManager());

        http
            .addFilter(authCheckerFilter)
            .sessionManagement().sessionCreationPolicy(STATELESS).and()
            .csrf().disable()
            .formLogin().disable()
            .logout().disable()
            .authorizeRequests()
            .antMatchers("/swagger-ui.html", "/webjars/springfox-swagger-ui/**", "/swagger-resources/**", "/v2/**", "/health").permitAll()
            .anyRequest().authenticated();
    }
}
