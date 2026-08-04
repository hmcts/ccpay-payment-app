package uk.gov.hmcts.payment.api.controllers.provider;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.payment.api.audit.AuditRepository;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.configuration.security.AuthenticatedServiceIdSupplier;
import uk.gov.hmcts.payment.api.controllers.PaymentReference;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.dto.mapper.CreditAccountDtoMapper;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.mapper.CreditAccountPaymentRequestMapper;
import uk.gov.hmcts.payment.api.mapper.PBAStatusErrorMapper;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.reports.FeesService;
import uk.gov.hmcts.payment.api.service.*;
import uk.gov.hmcts.payment.api.service.govpay.ServiceToTokenMap;
import uk.gov.hmcts.payment.api.util.ReferenceUtil;
import uk.gov.hmcts.payment.api.util.ServiceRequestCaseUtil;
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
        return new UserAwareDelegatingCreditAccountPaymentService(paymentFeeLinkRepository(), paymentChannelRepository, paymentMethodRepository,
            paymentProviderRepository, paymentStatusRepository,
            payment2Repository, referenceUtil(), serviceIdSupplier, userIdSupplier, serviceRequestCaseUtil);

    }

    @Bean("loggingCreditAccountPaymentService")
    @Primary
    public LoggingCreditAccountPaymentService loggingCreditAccountPaymentService() {
        return new LoggingCreditAccountPaymentService(userIdSupplier, userAwareDelegatingCreditAccountPaymentService());
    }

    @MockitoBean
    PaymentProviderRepository paymentProviderRepository;

    @MockitoBean
    PaymentStatusRepository paymentStatusRepository;
    @MockitoBean
    PaymentChannelRepository paymentChannelRepository;
    @MockitoBean
    PaymentMethodRepository paymentMethodRepository;
    @MockitoBean
    Payment2Repository payment2Repository;

    @Bean
    @Primary
    PaymentReference paymentReference(){

        return new PaymentReference(paymentFeeLinkRepository());
    }
    @MockitoBean
    ServiceIdSupplier serviceIdSupplier;
    @MockitoBean
    UserIdSupplier userIdSupplier;

    @MockitoBean
    DuplicatePaymentValidator paymentValidator;
    @MockitoBean
    FeePayApportionService feePayApportionService;
    @MockitoBean
    LaunchDarklyFeatureToggler featureToggler;
    @MockitoBean
    FeePayApportionRepository feePayApportionRepository;
    @MockitoBean
    PaymentFeeRepository paymentFeeRepository;
    @MockitoBean
    FeesService feesService;

    @MockitoBean
    AccountService<AccountDto, String> accountService;
    @MockitoBean
    DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService;
    @MockitoBean
    CallbackService callbackService;
    @MockitoBean
    TelephonyRepository telephonyRepository;
    @MockitoBean
    AuditRepository paymentAuditRepository;
    @MockitoBean
    ReferenceDataService referenceDataService;
    @MockitoBean
    AuthTokenGenerator authTokenGenerator;
    @MockitoBean
    ServiceRequestCaseUtil serviceRequestCaseUtil;

    @Bean
    @Primary
    public PaymentServiceImpl paymentService() {
        return new PaymentServiceImpl(delegatingPaymentService, payment2Repository, callbackService, paymentStatusRepository, telephonyRepository,
            paymentAuditRepository, feePayApportionService,
            feePayApportionRepository, featureToggler);
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
    public ServiceIdSupplier serviceIdSupplier() {
        return new AuthenticatedServiceIdSupplier();
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
