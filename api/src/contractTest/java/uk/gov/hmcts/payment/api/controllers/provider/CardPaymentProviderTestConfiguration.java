package uk.gov.hmcts.payment.api.controllers.provider;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;


@TestConfiguration
public class CardPaymentProviderTestConfiguration {

    @MockitoBean
    @Qualifier("restTemplatePaymentGroup")
    private RestTemplate restTemplate;

    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;

    @MockitoBean
    private ReferenceDataService referenceDataServiceImp;

    @Bean
    @Primary
    public PaymentDtoMapper paymentDtoMapper() {
        return new PaymentDtoMapper();
    }

    @Bean
    @Primary
    public UserAwareDelegatingPaymentService delegateUserPay() {
        return new UserAwareDelegatingPaymentService(userIdSupplier,
            paymentFeeLinkRepository, delegateGovPay(),
            delegatePciPal,
            paymentChannelRepository,
            paymentMethodRepository,
            paymentProviderRepository,
            paymentStatusRepository,
            paymentRespository,
            referenceUtil(),
            govPayAuthUtil,
            serviceIdSupplier(),
            auditRepository,
            callbackService,
            feePayApportionRepository,
            paymentFeeRepository,
            feePayApportionService,
            launchDarklyFeatureToggler,
            serviceRequestCaseUtil);
    }

    @Bean
    @Primary
    public PaymentServiceImpl paymentService() {
        return new PaymentServiceImpl(delegateUserPay(),
            paymentRespository,
            callbackService,
            paymentStatusRepository,
            telephonyRepository,
            auditRepository,
            feePayApportionService,
            feePayApportionRepository,
            launchDarklyFeatureToggler);
    }

    @Bean
    @Primary
    public ServiceToTokenMap serviceToTokenMap(){
        return new ServiceToTokenMap();
    }

    @Bean
    @Primary
    public GovPayDelegatingPaymentService delegateGovPay() {
        return new GovPayDelegatingPaymentService(govPayKeyRepository, govPayClient, serviceIdSupplier(), govPayAuthUtil, serviceToTokenMap());
    }

    @Bean
    public PaymentValidator paymentValidator() {
        return new PaymentValidator(dateUtil());
    }

    @Bean
    public DateUtil dateUtil() {
        return new DateUtil();
    }

    @MockitoBean
    public GovPayKeyRepository govPayKeyRepository;

    @MockitoBean
    public GovPayClient govPayClient;

    @MockitoBean
    public DelegatingPaymentService<PciPalPayment, String> delegatePciPal;


    @MockitoBean
    public uk.gov.hmcts.payment.api.reports.FeesService feeService;

    @MockitoBean
    public uk.gov.hmcts.fees2.register.data.service.FeeService feeService2;

    @MockitoBean
    public LaunchDarklyFeatureToggler launchDarklyFeatureToggler;
    @MockitoBean
    public UserIdSupplier userIdSupplier;
    @MockitoBean
    public PaymentFeeLinkRepository paymentFeeLinkRepository;
    @MockitoBean
    public PaymentFeeRepository paymentFeeRepository;
    @MockitoBean
    public PaymentStatusRepository paymentStatusRepository;
    @MockitoBean
    public PaymentChannelRepository paymentChannelRepository;
    @MockitoBean
    public PaymentProviderRepository paymentProviderRepository;
    @MockitoBean
    public PaymentMethodRepository paymentMethodRepository;
    @MockitoBean
    public Payment2Repository paymentRespository;
    @MockitoBean
    public CardDetailsService<CardDetails, String> cardDetailsService;
    @MockitoBean
    public PciPalPaymentService pciPalPaymentService;
    @MockitoBean
    FeePayApportionService feePayApportionService;

    @Bean
    @Primary
    public ReferenceUtil referenceUtil() {
        return new ReferenceUtil();
    }

    @MockitoBean
    public GovPayAuthUtil govPayAuthUtil;

    @Bean
    @Primary
    public ServiceIdSupplier serviceIdSupplier() {
        return new AuthenticatedServiceIdSupplier();
    }

    @MockitoBean
    public AuditRepository auditRepository;
    @MockitoBean
    public CallbackService callbackService;
    @MockitoBean
    public FeePayApportionRepository feePayApportionRepository;
    @MockitoBean
    public TelephonyRepository telephonyRepository;

    @MockitoBean
    ServiceRequestCaseUtil serviceRequestCaseUtil;

    @MockitoBean
    PaymentReference paymentReference;

}
