package uk.gov.hmcts.payment.api.controllers.provider;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.payment.api.domain.service.ServiceRequestDomainService;


@TestConfiguration
public class ServiceRequestProviderTestConfiguration {

    @MockBean
    public ServiceRequestDomainService serviceRequestDomainService;
}
