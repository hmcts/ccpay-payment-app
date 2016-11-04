package uk.gov.justice.payment.api.parameters.serviceid;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.ParameterBuilderPlugin;
import springfox.documentation.spi.service.contexts.ParameterContext;


@Component
public class ServiceIdSwaggerDocumentationPlugin implements ParameterBuilderPlugin {

    @Override
    public void apply(ParameterContext context) {
        MethodParameter methodParameter = context.methodParameter();
        methodParameter.getParameterAnnotations();

        if (methodParameter.hasParameterAnnotation(ServiceId.class)) {
            context.parameterBuilder().name("service_id").description("Service Id").parameterType("header");
        }
    }

    @Override
    public boolean supports(DocumentationType delimiter) {
        return DocumentationType.SWAGGER_2.equals(delimiter);
    }
}
