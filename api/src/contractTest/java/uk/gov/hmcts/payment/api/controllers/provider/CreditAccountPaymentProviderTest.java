package uk.gov.hmcts.payment.api.controllers.provider;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import java.io.IOException;
import java.util.Arrays;
import org.ff4j.FF4j;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.controllers.CreditAccountPaymentController;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.dto.mapper.CreditAccountDtoMapper;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.mapper.CreditAccountPaymentRequestMapper;
import uk.gov.hmcts.payment.api.mapper.PBAStatusErrorMapper;
import uk.gov.hmcts.payment.api.model.FeePayApportionRepository;
import uk.gov.hmcts.payment.api.model.PaymentChannel;
import uk.gov.hmcts.payment.api.model.PaymentChannelRepository;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.model.PaymentFeeRepository;
import uk.gov.hmcts.payment.api.model.PaymentMethod;
import uk.gov.hmcts.payment.api.model.PaymentMethodRepository;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.model.PaymentStatusRepository;
import uk.gov.hmcts.payment.api.service.AccountService;
import uk.gov.hmcts.payment.api.service.CreditAccountPaymentService;
import uk.gov.hmcts.payment.api.service.FeePayApportionService;
import uk.gov.hmcts.payment.api.v1.model.PaymentRepository;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;
import uk.gov.hmcts.payment.api.validators.DuplicatePaymentValidator;

@ExtendWith(SpringExtension.class)
@Provider("payment_creditAccountPayment")
@PactBroker(scheme = "${PACT_BROKER_SCHEME:http}", host = "${PACT_BROKER_URL:localhost}", port = "${PACT_BROKER_PORT:80}")
@Import(CreditAccountPaymentProviderTestConfiguration.class)
public class CreditAccountPaymentProviderTest {


    @Autowired
    PaymentDtoMapper paymentDtoMapper;
    @Autowired
    CreditAccountPaymentService<PaymentFeeLink, String> creditAccountPaymentService;
    @Autowired
    CreditAccountDtoMapper creditAccountDtoMapper;
    @Autowired
    AccountService<AccountDto, String> accountService;
    @Autowired
    DuplicatePaymentValidator paymentValidator;
    @Autowired
    FeePayApportionRepository feePayApportionRepository;
    @Autowired
    PaymentFeeRepository paymentFeeRepository;
    @Autowired
    FF4j ff4j;
    @Autowired
    FeePayApportionService feePayApportionService;
    @Autowired
    LaunchDarklyFeatureToggler featureToggler;

    @Autowired
    UserIdSupplier userIdSupplierMock;

    @Autowired
    ServiceIdSupplier serviceIdSupplierMock;

    @Autowired
    PaymentChannelRepository paymentChannelRepositoryMock;

    @Autowired
    PaymentMethodRepository paymentMethodRepositoryMock;

    @Autowired
    PaymentStatusRepository paymentStatusRepositoryMock;

    @Autowired
    PaymentFeeLinkRepository paymentFeeLinkRepositoryMock;

    @Autowired
    PBAStatusErrorMapper pbaStatusErrorMapper;

    @Autowired
    CreditAccountPaymentRequestMapper requestMapper;

    private final static String PAYMENT_CHANNEL_ONLINE = "online";

    private final static String PAYMENT_METHOD = "payment by account";


    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }


    @BeforeEach
    void before(PactVerificationContext context) {

        System.getProperties().setProperty("pact.verifier.publishResults", "true");
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        testTarget.setControllers(new CreditAccountPaymentController(creditAccountPaymentService, creditAccountDtoMapper, accountService, paymentValidator, feePayApportionService, featureToggler, pbaStatusErrorMapper, requestMapper, Arrays.asList("DIVORCE")));
        context.setTarget(testTarget);
    }

    @State({"A credit account payment does not exist"})
    public void toCreateNewCreditAccountPayment() throws IOException, JSONException {
        when(userIdSupplierMock.get()).thenReturn("userId");
        when(serviceIdSupplierMock.get()).thenReturn("ccd_gw");
        when(paymentChannelRepositoryMock.findByNameOrThrow(PAYMENT_CHANNEL_ONLINE)).thenReturn(PaymentChannel.paymentChannelWith().description("Online Channel").name("online").build());
        when(paymentMethodRepositoryMock.findByNameOrThrow(PAYMENT_METHOD)).thenReturn(PaymentMethod.paymentMethodWith().description("Payment by account").name("Payment by account").build());
        when(paymentStatusRepositoryMock.findByNameOrThrow(anyString())).thenReturn(PaymentStatus.paymentStatusWith().description("Payment Status success").name("success").build());

    }


}
