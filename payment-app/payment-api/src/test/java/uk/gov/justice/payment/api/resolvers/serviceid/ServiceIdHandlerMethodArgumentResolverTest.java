package uk.gov.justice.payment.api.resolvers.serviceid;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.web.context.request.NativeWebRequest;
import uk.gov.justice.payment.api.GovPayConfig;
import uk.gov.justice.payment.api.parameters.serviceid.ServiceIdHandlerMethodArgumentResolver;
import uk.gov.justice.payment.api.parameters.serviceid.UnknownServiceIdException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServiceIdHandlerMethodArgumentResolverTest {

    private GovPayConfig govPayConfig = Mockito.mock(GovPayConfig.class);
    private NativeWebRequest webRequest = mock(NativeWebRequest.class);

    @Test(expected = UnknownServiceIdException.class)
    public void throwsExceptionIfUnknownServiceIdProvided() throws Exception {
        when(govPayConfig.hasKeyForService("unknownServiceId")).thenReturn(false);
        when(webRequest.getHeader("service_id")).thenReturn("unknownServiceId");

        new ServiceIdHandlerMethodArgumentResolver(govPayConfig).resolveArgument(null, null, webRequest, null);
    }

    @Test
    public void returnsServiceIdFromHeader() throws Exception {
        when(govPayConfig.hasKeyForService("knownServiceId")).thenReturn(true);
        when(webRequest.getHeader("service_id")).thenReturn("knownServiceId");

        Assertions.assertThat(new ServiceIdHandlerMethodArgumentResolver(govPayConfig).resolveArgument(null, null, webRequest, null))
                .isEqualTo("knownServiceId");
    }
}