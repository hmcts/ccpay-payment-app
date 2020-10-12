package uk.gov.hmcts.payment.api.controllers.provider;

import org.ff4j.FF4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.configuration.security.AuthenticatedServiceIdSupplier;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.dto.mapper.CreditAccountDtoMapper;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.model.FeePayApportionRepository;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentChannelRepository;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.model.PaymentFeeRepository;
import uk.gov.hmcts.payment.api.model.PaymentMethodRepository;
import uk.gov.hmcts.payment.api.model.PaymentProviderRepository;
import uk.gov.hmcts.payment.api.model.PaymentStatusRepository;
import uk.gov.hmcts.payment.api.reports.FeesService;
import uk.gov.hmcts.payment.api.service.AccountService;
import uk.gov.hmcts.payment.api.service.FeePayApportionService;
import uk.gov.hmcts.payment.api.service.LoggingCreditAccountPaymentService;
import uk.gov.hmcts.payment.api.service.MockAccountServiceImpl;
import uk.gov.hmcts.payment.api.service.UserAwareDelegatingCreditAccountPaymentService;
import uk.gov.hmcts.payment.api.util.ReferenceUtil;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;
import uk.gov.hmcts.payment.api.validators.DuplicatePaymentValidator;


@TestConfiguration
public class CreditAccountPaymentProviderTestConfiguration {

    @Bean
    @Primary
    public PaymentDtoMapper paymentDtoMapper() {
        return new PaymentDtoMapper();
    }


    @Bean
    public UserAwareDelegatingCreditAccountPaymentService userAwareDelegatingCreditAccountPaymentService() {
        return new UserAwareDelegatingCreditAccountPaymentService(paymentFeeLinkRepository(), paymentChannelRepository, paymentMethodRepository, paymentProviderRepository, paymentStatusRepository, paymentRespository, referenceUtil(), serviceIdSupplier, userIdSupplier);

    }

    @Bean("loggingCreditAccountPaymentService")
    @Primary
    public LoggingCreditAccountPaymentService loggingCreditAccountPaymentService() {
        return new LoggingCreditAccountPaymentService(userIdSupplier, userAwareDelegatingCreditAccountPaymentService());
    }

    @MockBean
    PaymentProviderRepository paymentProviderRepository;

    @MockBean
    PaymentStatusRepository paymentStatusRepository;
    @MockBean
    PaymentChannelRepository paymentChannelRepository;
    @MockBean
    PaymentMethodRepository paymentMethodRepository;
    @MockBean
    Payment2Repository paymentRespository;
    @MockBean
    ServiceIdSupplier serviceIdSupplier;
    @MockBean
    UserIdSupplier userIdSupplier;

    @MockBean
    DuplicatePaymentValidator paymentValidator;
    @MockBean
    FF4j ff4j;
    @MockBean
    FeePayApportionService feePayApportionService;
    @MockBean
    LaunchDarklyFeatureToggler featureToggler;
    @MockBean
    FeePayApportionRepository feePayApportionRepository;
    @MockBean
    PaymentFeeRepository paymentFeeRepository;
    @MockBean
    FeesService feesService;

    @Bean
    @Primary
    public AccountService<AccountDto, String> accountService() {
        return new MockAccountServiceImpl();
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

    ;


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


}
