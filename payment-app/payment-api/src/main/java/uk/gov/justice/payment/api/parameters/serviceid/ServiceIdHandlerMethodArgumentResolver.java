package uk.gov.justice.payment.api.parameters.serviceid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import uk.gov.justice.payment.api.configuration.GovPayConfig;

@Component
public class ServiceIdHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

    private final GovPayConfig govPayConfig;

    @Autowired
    public ServiceIdHandlerMethodArgumentResolver(GovPayConfig govPayConfig) {
        this.govPayConfig = govPayConfig;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(String.class) && parameter.hasParameterAnnotation(ServiceId.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        String serviceId = webRequest.getHeader("service_id");

        if (!govPayConfig.hasKeyForService(serviceId)) {
            throw new UnknownServiceIdException(serviceId);
        }

        return serviceId;
    }
}
