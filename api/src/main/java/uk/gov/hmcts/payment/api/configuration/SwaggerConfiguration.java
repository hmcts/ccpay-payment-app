package uk.gov.hmcts.payment.api.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.payment.api.controllers.PaymentExternalAPI;

@Configuration
public class SwaggerConfiguration {

    private static final String HEADER = "header";

    @Bean
    public GroupedOpenApi paymentApi() {
        return GroupedOpenApi.builder()
            .group("payment")
            .packagesToScan("uk.gov.hmcts.payment.api.v1.controllers")
            .pathsToMatch("/**")
            .addOperationCustomizer(authorizationHeaders())
            .build();
    }

    @Bean
    public GroupedOpenApi payment2Api() {
        return GroupedOpenApi.builder()
            .group("payment2")
            .packagesToScan("uk.gov.hmcts.payment.api.controllers")
            .pathsToMatch("/**")
            .addOperationCustomizer(authorizationHeaders())
            .build();
    }

    @Bean
    public GroupedOpenApi paymentReferenceDataApi() {
        return GroupedOpenApi.builder()
            .group("reference-data")
            .packagesToScan("uk.gov.hmcts.payment.referencedata.controllers")
            .pathsToMatch("/**")
            .addOperationCustomizer(authorizationHeaders())
            .build();
    }

    @Bean
    public GroupedOpenApi externalApi() {
        return GroupedOpenApi.builder()
            .group("payment-external-api")
            .addOpenApiMethodFilter(method -> method.isAnnotationPresent(PaymentExternalAPI.class))
            .addOperationCustomizer(authorizationHeaders())
            .build();
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .openapi("3.0.1")
            .components(new Components())
            .info(new Info().title("Payment App").version("1.0.0"));
    }

    @Bean
    public OperationCustomizer authorizationHeaders() {
        return (operation, handlerMethod) -> {
            addHeaderIfAbsent(operation, "Authorization", "User authorization header");
            addHeaderIfAbsent(operation, "ServiceAuthorization", "Service authorization header");
            return operation;
        };
    }

    // An endpoint that declares the header itself (e.g. as optional) keeps its own definition rather than getting a
    // second, mandatory one.
    private void addHeaderIfAbsent(Operation operation, String name, String description) {
        boolean declared = operation.getParameters() != null && operation.getParameters().stream()
            .anyMatch(p -> HEADER.equals(p.getIn()) && name.equalsIgnoreCase(p.getName()));
        if (!declared) {
            operation.addParametersItem(mandatoryStringParameter(name, description));
        }
    }

    private Parameter mandatoryStringParameter(String name, String description) {
        return new Parameter()
            .name(name)
            .description(description)
            .required(true)
            .in(HEADER)
            .schema(new StringSchema());
    }
}
