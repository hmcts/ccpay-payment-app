package uk.gov.justice.payment.api.configuration;

import io.swagger.annotations.ApiParam;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.ParameterBuilderPlugin;
import springfox.documentation.spi.service.contexts.ParameterContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import static hidden.org.codehaus.plexus.interpolation.util.StringUtils.capitalizeFirstLetter;
import static org.apache.commons.lang.StringUtils.join;
import static org.apache.commons.lang.StringUtils.splitByCharacterTypeCamelCase;
import static springfox.documentation.builders.PathSelectors.regex;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket newsApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("payment")
                .apiInfo(apiInfo())
                .select()
                .paths(regex("/payment.*"))
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("Payment API documentation")
                .description("Payment API documentation")
                .build();
    }

    @Component
    public static class CamelCaseToSpacesDocumentationPlugin implements ParameterBuilderPlugin {

        @Override
        public void apply(ParameterContext context) {
            MethodParameter methodParameter = context.methodParameter();

            if (methodParameter.hasParameterAnnotation(ApiParam.class)) {
                return;
            }

            String description = capitalizeFirstLetter(join(splitByCharacterTypeCamelCase(methodParameter.getParameterName()), ' '));
            context.parameterBuilder().description(description);
        }

        @Override
        public boolean supports(DocumentationType delimiter) {
            return DocumentationType.SWAGGER_2.equals(delimiter);
        }
    }
}
