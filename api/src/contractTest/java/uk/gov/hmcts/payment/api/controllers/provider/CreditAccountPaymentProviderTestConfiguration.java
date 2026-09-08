package uk.gov.hmcts.payment.api.controllers.provider;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import uk.gov.hmcts.payment.api.audit.AuditRepository;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.controllers.PaymentReference;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.dto.mapper.CreditAccountDtoMapper;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.mapper.CreditAccountPaymentRequestMapper;
import uk.gov.hmcts.payment.api.mapper.PBAStatusErrorMapper;
import uk.gov.hmcts.payment.api.model.FeePayApportionRepository;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentChannelRepository;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.model.PaymentFeeRepository;
import uk.gov.hmcts.payment.api.model.PaymentMethodRepository;
import uk.gov.hmcts.payment.api.model.PaymentProviderRepository;
import uk.gov.hmcts.payment.api.model.PaymentStatusRepository;
import uk.gov.hmcts.payment.api.model.TelephonyRepository;
import uk.gov.hmcts.payment.api.reports.FeesService;
import uk.gov.hmcts.payment.api.service.AccountService;
import uk.gov.hmcts.payment.api.service.CallbackService;
import uk.gov.hmcts.payment.api.service.DelegatingPaymentService;
import uk.gov.hmcts.payment.api.service.FeePayApportionService;
import uk.gov.hmcts.payment.api.service.LoggingCreditAccountPaymentService;
import uk.gov.hmcts.payment.api.service.PaymentServiceImpl;
import uk.gov.hmcts.payment.api.service.ReferenceDataService;
import uk.gov.hmcts.payment.api.service.UserAwareDelegatingCreditAccountPaymentService;
import uk.gov.hmcts.payment.api.service.govpay.ServiceToTokenMap;
import uk.gov.hmcts.payment.api.util.ServiceRequestCaseUtil;
import uk.gov.hmcts.payment.api.util.ReferenceUtil;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;
import uk.gov.hmcts.payment.api.validators.DuplicatePaymentValidator;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;


@TestConfiguration
public class CreditAccountPaymentProviderTestConfiguration {

    @Bean
    @Primary
    public PaymentDtoMapper paymentDtoMapper() {
        return new PaymentDtoMapper();
    }

    @Bean
    public UserAwareDelegatingCreditAccountPaymentService userAwareDelegatingCreditAccountPaymentService() {
        return new UserAwareDelegatingCreditAccountPaymentService(paymentFeeLinkRepository(), paymentChannelRepository(), paymentMethodRepository(),
            paymentProviderRepository(), paymentStatusRepository(),
            payment2Repository(), referenceUtil(), serviceIdSupplier(), userIdSupplier(), serviceRequestCaseUtil());

    }

    @Bean("loggingCreditAccountPaymentService")
    @Primary
    public LoggingCreditAccountPaymentService loggingCreditAccountPaymentService() {
        return new LoggingCreditAccountPaymentService(userIdSupplier(), userAwareDelegatingCreditAccountPaymentService());
    }

    @Bean
    public PaymentProviderRepository paymentProviderRepository() {
        return Mockito.mock(PaymentProviderRepository.class);
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
    public PaymentMethodRepository paymentMethodRepository() {
        return Mockito.mock(PaymentMethodRepository.class);
    }

    @Bean
    public Payment2Repository payment2Repository() {
        return Mockito.mock(Payment2Repository.class);
    }

    @Bean
    @Primary
    PaymentReference paymentReference() {
        return new PaymentReference(paymentFeeLinkRepository());
    }

    @Bean
    public ServiceIdSupplier serviceIdSupplier() {
        return Mockito.mock(ServiceIdSupplier.class);
    }

    @Bean
    public UserIdSupplier userIdSupplier() {
        return Mockito.mock(UserIdSupplier.class);
    }

    @Bean
    public DuplicatePaymentValidator paymentValidator() {
        return Mockito.mock(DuplicatePaymentValidator.class);
    }

    @Bean
    public FeePayApportionService feePayApportionService() {
        return Mockito.mock(FeePayApportionService.class);
    }

    @Bean
    public LaunchDarklyFeatureToggler featureToggler() {
        return Mockito.mock(LaunchDarklyFeatureToggler.class);
    }

    @Bean
    public FeePayApportionRepository feePayApportionRepository() {
        return Mockito.mock(FeePayApportionRepository.class);
    }

    @Bean
    public PaymentFeeRepository paymentFeeRepository() {
        return Mockito.mock(PaymentFeeRepository.class);
    }

    @Bean
    public FeesService feesService() {
        return Mockito.mock(FeesService.class);
    }

    @Bean
    public AccountService<AccountDto, String> accountService() {
        return Mockito.mock(AccountService.class);
    }

    @Bean
    public DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService() {
        return Mockito.mock(DelegatingPaymentService.class);
    }

    @Bean
    public CallbackService callbackService() {
        return Mockito.mock(CallbackService.class);
    }

    @Bean
    public TelephonyRepository telephonyRepository() {
        return Mockito.mock(TelephonyRepository.class);
    }

    @Bean
    public AuditRepository paymentAuditRepository() {
        return Mockito.mock(AuditRepository.class);
    }

    @Bean
    public ReferenceDataService referenceDataService() {
        return Mockito.mock(ReferenceDataService.class);
    }

    @Bean
    public AuthTokenGenerator authTokenGenerator() {
        return Mockito.mock(AuthTokenGenerator.class);
    }

    @Bean
    public ServiceRequestCaseUtil serviceRequestCaseUtil() {
        return Mockito.mock(ServiceRequestCaseUtil.class);
    }

    @Bean
    @Primary
    public PaymentServiceImpl paymentService() {
        return new PaymentServiceImpl(delegatingPaymentService(), payment2Repository(), callbackService(), paymentStatusRepository(), telephonyRepository(),
            paymentAuditRepository(), feePayApportionService(),
            feePayApportionRepository(), featureToggler());
    }

    @Bean
    @Primary
    public ReferenceUtil referenceUtil() {
        return new ReferenceUtil();
    }

    @Bean
    @Primary
    public CreditAccountDtoMapper creditAccountDtoMapper() {
        return new CreditAccountDtoMapper();
    }

    @Bean
    @Primary
    public PaymentFeeLinkRepository paymentFeeLinkRepository() {
        return new PayFeeLinkRepositoryStub();
    }

    @Bean
    @Primary
    public PBAStatusErrorMapper pBAStatusErrorMapper() {
        return new PBAStatusErrorMapper();
    }

    @Bean
    @Primary
    CreditAccountPaymentRequestMapper requestMapper() {
        return new CreditAccountPaymentRequestMapper();
    }

    @Bean
    @Primary
    ServiceToTokenMap serviceToTokenMap() { return new ServiceToTokenMap(); }

}
