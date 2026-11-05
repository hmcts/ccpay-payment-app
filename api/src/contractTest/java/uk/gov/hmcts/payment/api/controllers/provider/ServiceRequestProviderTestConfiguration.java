package uk.gov.hmcts.payment.api.controllers.provider;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import uk.gov.hmcts.payment.api.domain.service.ServiceRequestDomainService;


@TestConfiguration
public class ServiceRequestProviderTestConfiguration {

    @Bean
    public ServiceRequestDomainService serviceRequestDomainService() {
        return Mockito.mock(ServiceRequestDomainService.class);
    }
}
