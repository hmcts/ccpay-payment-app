package uk.gov.hmcts.payment.api.controllers.provider;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.payment.api.audit.AuditRepository;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.configuration.security.AuthenticatedServiceIdSupplier;
import uk.gov.hmcts.payment.api.controllers.PaymentReference;
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
import uk.gov.hmcts.payment.api.reports.FeesService;
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
import uk.gov.hmcts.payment.api.util.ServiceRequestCaseUtil;
import uk.gov.hmcts.payment.api.util.ReferenceUtil;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.govpay.GovPayAuthUtil;
import uk.gov.hmcts.payment.api.v1.model.govpay.GovPayKeyRepository;
import uk.gov.hmcts.payment.api.validators.PaymentValidator;
import uk.gov.hmcts.fees2.register.data.service.FeeService;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;


@TestConfiguration
public class CardPaymentProviderTestConfiguration {

    @Bean
    @Qualifier("restTemplatePaymentGroup")
    public RestTemplate restTemplate() {
        return Mockito.mock(RestTemplate.class);
    }

    @Bean
    public AuthTokenGenerator authTokenGenerator() {
        return Mockito.mock(AuthTokenGenerator.class);
    }

    @Bean
    public ReferenceDataService referenceDataServiceImp() {
        return Mockito.mock(ReferenceDataService.class);
    }

    @Bean
    @Primary
    public PaymentDtoMapper paymentDtoMapper() {
        return new PaymentDtoMapper();
    }

    @Bean
    @Primary
    public UserAwareDelegatingPaymentService delegateUserPay() {
        return new UserAwareDelegatingPaymentService(userIdSupplier(),
            paymentFeeLinkRepository(), delegateGovPay(),
            delegatePciPal(),
            paymentChannelRepository(),
            paymentMethodRepository(),
            paymentProviderRepository(),
            paymentStatusRepository(),
            paymentRespository(),
            referenceUtil(),
            govPayAuthUtil(),
            serviceIdSupplier(),
            auditRepository(),
            callbackService(),
            feePayApportionRepository(),
            paymentFeeRepository(),
            feePayApportionService(),
            launchDarklyFeatureToggler(),
            serviceRequestCaseUtil());
    }

    @Bean
    @Primary
    public PaymentServiceImpl paymentService() {
        return new PaymentServiceImpl(delegateUserPay(),
            paymentRespository(),
            callbackService(),
            paymentStatusRepository(),
            telephonyRepository(),
            auditRepository(),
            feePayApportionService(),
            feePayApportionRepository(),
            launchDarklyFeatureToggler());
    }

    @Bean
    @Primary
    public ServiceToTokenMap serviceToTokenMap() {
        return new ServiceToTokenMap();
    }

    @Bean
    @Primary
    public GovPayDelegatingPaymentService delegateGovPay() {
        return new GovPayDelegatingPaymentService(govPayKeyRepository(), govPayClient(), serviceIdSupplier(), govPayAuthUtil(), serviceToTokenMap());
    }

    @Bean
    public PaymentValidator paymentValidator() {
        return new PaymentValidator(dateUtil());
    }

    @Bean
    public DateUtil dateUtil() {
        return new DateUtil();
    }

    @Bean
    public GovPayKeyRepository govPayKeyRepository() {
        return Mockito.mock(GovPayKeyRepository.class);
    }

    @Bean
    public GovPayClient govPayClient() {
        return Mockito.mock(GovPayClient.class);
    }

    @Bean
    public DelegatingPaymentService<PciPalPayment, String> delegatePciPal() {
        return Mockito.mock(DelegatingPaymentService.class);
    }

    @Bean
    public FeesService feeService() {
        return Mockito.mock(FeesService.class);
    }

    @Bean
    public FeeService feeService2() {
        return Mockito.mock(FeeService.class);
    }

    @Bean
    public LaunchDarklyFeatureToggler launchDarklyFeatureToggler() {
        return Mockito.mock(LaunchDarklyFeatureToggler.class);
    }

    @Bean
    public UserIdSupplier userIdSupplier() {
        return Mockito.mock(UserIdSupplier.class);
    }

    @Bean
    public PaymentFeeLinkRepository paymentFeeLinkRepository() {
        return Mockito.mock(PaymentFeeLinkRepository.class);
    }

    @Bean
    public PaymentFeeRepository paymentFeeRepository() {
        return Mockito.mock(PaymentFeeRepository.class);
    }

    @Bean
    public PaymentStatusRepository paymentStatusRepository() {
        return Mockito.mock(PaymentStatusRepository.class);
    }

    @Bean
    public PaymentChannelRepository paymentChannelRepository() {
        return Mockito.mock(PaymentChannelRepository.class);
    }

    @Bean
    public PaymentProviderRepository paymentProviderRepository() {
        return Mockito.mock(PaymentProviderRepository.class);
    }

    @Bean
    public PaymentMethodRepository paymentMethodRepository() {
        return Mockito.mock(PaymentMethodRepository.class);
    }

    @Bean
    public Payment2Repository paymentRespository() {
        return Mockito.mock(Payment2Repository.class);
    }

    @Bean
    public CardDetailsService<CardDetails, String> cardDetailsService() {
        return Mockito.mock(CardDetailsService.class);
    }

    @Bean
    public PciPalPaymentService pciPalPaymentService() {
        return Mockito.mock(PciPalPaymentService.class);
    }

    @Bean
    public FeePayApportionService feePayApportionService() {
        return Mockito.mock(FeePayApportionService.class);
    }

    @Bean
    @Primary
    public ReferenceUtil referenceUtil() {
        return new ReferenceUtil();
    }

    @Bean
    public GovPayAuthUtil govPayAuthUtil() {
        return Mockito.mock(GovPayAuthUtil.class);
    }

    @Bean
    @Primary
    public ServiceIdSupplier serviceIdSupplier() {
        return new AuthenticatedServiceIdSupplier();
    }

    @Bean
    public AuditRepository auditRepository() {
        return Mockito.mock(AuditRepository.class);
    }

    @Bean
    public CallbackService callbackService() {
        return Mockito.mock(CallbackService.class);
    }

    @Bean
    public FeePayApportionRepository feePayApportionRepository() {
        return Mockito.mock(FeePayApportionRepository.class);
    }

    @Bean
    public TelephonyRepository telephonyRepository() {
        return Mockito.mock(TelephonyRepository.class);
    }

    @Bean
    public ServiceRequestCaseUtil serviceRequestCaseUtil() {
        return Mockito.mock(ServiceRequestCaseUtil.class);
    }

    @Bean
    public PaymentReference paymentReference() {
        return Mockito.mock(PaymentReference.class);
    }

}