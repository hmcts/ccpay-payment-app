package uk.gov.hmcts.payment.api.configuration.security;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
public class PaymentAuthCheckerConfiguration {

    @Value("#{'${trusted.s2s.service.names}'.split(',')}")
    private List<String> authorizedServices;

    @Bean
    public Function<HttpServletRequest, Optional<String>> userIdExtractor() {
        Pattern pattern = Pattern.compile("^/users/([^/]+)/.+$");

        return (request) -> {
            Matcher matcher = pattern.matcher(request.getRequestURI());
            boolean matched = matcher.find();
            return Optional.ofNullable(matched ? matcher.group(1) : null);
        };
    }

    @Bean(value = "authorizedRolesExtractor")
    public Function<HttpServletRequest, Collection<String>> authorizedRolesExtractor() {
        return (any) -> Collections.emptyList();
    }

    @Bean(value = "authorizedServiceExtractor")
    public Function<HttpServletRequest, Collection<String>> authorizedServicesExtractor() {
        return (any) -> authorizedServices;
    }
}
