package uk.gov.hmcts.payment.api.controllers.provider;

import org.ff4j.FF4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.audit.AuditRepository;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.configuration.security.AuthenticatedServiceIdSupplier;
import uk.gov.hmcts.payment.api.domain.service.ServiceRequestDomainService;
import uk.gov.hmcts.payment.api.dto.PciPalPayment;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.external.client.GovPayClient;
import uk.gov.hmcts.payment.api.external.client.dto.CardDetails;
import uk.gov.hmcts.payment.api.model.FeePayApportionRepository;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentChannelRepository;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.model.PaymentFeeRepository;
import uk.gov.hmcts.payment.api.model.PaymentMethodRepository;
import uk.gov.hmcts.payment.api.model.PaymentProviderRepository;
import uk.gov.hmcts.payment.api.model.PaymentStatusRepository;
import uk.gov.hmcts.payment.api.model.TelephonyRepository;
import uk.gov.hmcts.payment.api.service.CallbackService;
import uk.gov.hmcts.payment.api.service.CardDetailsService;
import uk.gov.hmcts.payment.api.service.DelegatingPaymentService;
import uk.gov.hmcts.payment.api.service.FeePayApportionService;
import uk.gov.hmcts.payment.api.service.PaymentServiceImpl;
import uk.gov.hmcts.payment.api.service.PciPalPaymentService;
import uk.gov.hmcts.payment.api.service.ReferenceDataService;
import uk.gov.hmcts.payment.api.service.UserAwareDelegatingPaymentService;
import uk.gov.hmcts.payment.api.service.govpay.GovPayDelegatingPaymentService;
import uk.gov.hmcts.payment.api.service.govpay.ServiceToTokenMap;
import uk.gov.hmcts.payment.api.util.DateUtil;
import uk.gov.hmcts.payment.api.util.ReferenceUtil;
import uk.gov.hmcts.payment.api.util.ServiceRequestCaseUtil;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.govpay.GovPayAuthUtil;
import uk.gov.hmcts.payment.api.v1.model.govpay.GovPayKeyRepository;
import uk.gov.hmcts.payment.api.validators.PaymentValidator;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;


@TestConfiguration
public class ServiceRequestProviderTestConfiguration {

    @MockBean
    public ServiceRequestDomainService serviceRequestDomainService;
}
