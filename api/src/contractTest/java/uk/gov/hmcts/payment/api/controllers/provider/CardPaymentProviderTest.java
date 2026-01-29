package uk.gov.hmcts.payment.api.controllers.provider;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import com.google.common.collect.ImmutableMap;
import org.ff4j.FF4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.fees2.register.data.service.FeeService;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.controllers.CardPaymentController;
import uk.gov.hmcts.payment.api.controllers.PaymentController;
import uk.gov.hmcts.payment.api.controllers.PaymentReference;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.external.client.GovPayClient;
import uk.gov.hmcts.payment.api.external.client.dto.CardDetails;
import uk.gov.hmcts.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.Link;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentChannel;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.model.PaymentFeeRepository;
import uk.gov.hmcts.payment.api.model.PaymentMethod;
import uk.gov.hmcts.payment.api.model.PaymentProvider;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.model.PaymentStatusRepository;
import uk.gov.hmcts.payment.api.model.StatusHistory;
import uk.gov.hmcts.payment.api.service.CallbackService;
import uk.gov.hmcts.payment.api.service.CardDetailsService;
import uk.gov.hmcts.payment.api.service.DelegatingPaymentService;
import uk.gov.hmcts.payment.api.service.FeePayApportionService;
import uk.gov.hmcts.payment.api.service.PaymentServiceImpl;
import uk.gov.hmcts.payment.api.service.PciPalPaymentService;
import uk.gov.hmcts.payment.api.service.ReferenceDataService;
import uk.gov.hmcts.payment.api.util.DateUtil;
import uk.gov.hmcts.payment.api.util.ReferenceUtil;
import uk.gov.hmcts.payment.api.v1.model.govpay.GovPayAuthUtil;
import uk.gov.hmcts.payment.api.v1.model.govpay.GovPayKeyRepository;
import uk.gov.hmcts.payment.api.validators.PaymentValidator;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.payment.api.model.PaymentFee.feeWith;
import static uk.gov.hmcts.payment.api.model.PaymentFeeLink.paymentFeeLinkWith;

// Explicit consumer list included to handle DIAC-267, can be removed once DIAC-267 is done.
// Failing consumers ia_caseDocumentsApi and ia_casePaymentsApi.
// https://tools.hmcts.net/jira/browse/DIAC-267
@ExtendWith(SpringExtension.class)
@Provider("payment_cardPayment")
@EnabledIfEnvironmentVariable(named = "PACT_BROKER_URL", matches = ".+")
@PactBroker(scheme = "${PACT_BROKER_SCHEME:http}", host = "${PACT_BROKER_URL:localhost}", port = "${PACT_BROKER_PORT:80}", consumerVersionSelectors = {
    @VersionSelector(consumer = "civil_service", tag = "master"),
    @VersionSelector(consumer = "divorce_caseOrchestratorService", tag = "master"),
    @VersionSelector(consumer = "fpl_ccdConfiguration", tag = "master"),
    @VersionSelector(consumer = "fr_caseOrchestratorService", tag = "master"),
    @VersionSelector(consumer = "payment_App", tag = "master"),
    @VersionSelector(consumer = "pcs_api", tag = "master"),
    @VersionSelector(consumer = "prl_cos", tag = "master"),
    @VersionSelector(consumer = "probate_backOffice", tag = "master"),
    @VersionSelector(consumer = "probate_caveatsFrontEnd", tag = "master"),
    @VersionSelector(consumer = "xui_manageOrg", tag = "master")
})
@Import(CardPaymentProviderTestConfiguration.class)
@IgnoreNoPactsToVerify
class CardPaymentProviderTest {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    PaymentDtoMapper paymentDtoMapper;
    @Autowired
    FeeService feeService;
    @Autowired
    CardDetailsService<CardDetails, String> cardDetailsService;
    @Autowired
    PciPalPaymentService pciPalPaymentService;
    @Autowired
    FF4j ff4j;
    @Autowired
    FeePayApportionService feePayApportionService;

    @Autowired
    PaymentFeeRepository paymentFeeRepositoryMock;

    @Autowired
    PaymentFeeLinkRepository paymentFeeLinkRepositoryMock;

    @Autowired
    LaunchDarklyFeatureToggler featureToggler;

    @Autowired
    ReferenceDataService referenceDataService;

    @Autowired
    DelegatingPaymentService<PaymentFeeLink, String> cardDelegatingPaymentService;

    @Autowired
    PaymentServiceImpl paymentService;

    @Autowired
    PaymentValidator paymentValidator;

    @Autowired
    DateUtil dateUtil;

    @Autowired
    Payment2Repository payment2RepositoryMock;

    @Autowired
    PaymentStatusRepository paymentStatusRepositoryMock;

    @Autowired
    CallbackService callbackServiceMock;


    @Autowired
    GovPayClient govPayClientMock;

    @Autowired
    GovPayAuthUtil govPayAuthUtil;

    @Autowired
    ReferenceUtil referenceUtil;

    @Autowired
    GovPayKeyRepository govPayKeyRepositoryMock;

    @Autowired
    PaymentReference paymentReferenceMock;

    @Value("${PACT_BRANCH_NAME}")
    String branchName;


    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        System.getProperties().setProperty("pact.verifier.publishResults", "true");
        // Set provider version for publishing verification results
        String gitCommit = System.getenv().getOrDefault("GIT_COMMIT", getGitCommitHash());
        System.getProperties().setProperty("pact.provider.version", gitCommit);
        System.getProperties().setProperty("pact.provider.branch", branchName != null ? branchName : "master");

        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        when(paymentReferenceMock.getNext()).thenReturn("123");
        CardPaymentController cardPaymentController =
            new CardPaymentController(cardDelegatingPaymentService, paymentDtoMapper, cardDetailsService, pciPalPaymentService, ff4j,
                feePayApportionService, featureToggler, referenceDataService, paymentReferenceMock);
        cardPaymentController.setApplicationContext(applicationContext);
        testTarget.setControllers(
            cardPaymentController,
            new PaymentController(paymentService, paymentStatusRepositoryMock, callbackServiceMock,
                paymentDtoMapper, paymentValidator, ff4j,
                dateUtil, paymentFeeRepositoryMock, featureToggler));
        if (context != null) {
            context.setTarget(testTarget);
        }

    }

    private String getGitCommitHash() {
        try {
            Process process = Runtime.getRuntime().exec("git rev-parse --verify --short HEAD");
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));
            String hash = reader.readLine();
            reader.close();
            return hash != null ? hash.trim() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    @State({"A payment reference exists"})
    public void toGetCardPaymentDetailsWithSuccess() {
        when(payment2RepositoryMock.findByReference("654321ABC"))
            .thenReturn(Optional.of(populateCardPaymentToDb("1", "e2kkddts5215h9qqoeuth5c0v", "ccd_gw")));
        when(govPayAuthUtil.getServiceName(null, "ccd_gw")).thenReturn("ccd_gw");
        when(govPayAuthUtil.getServiceToken("ccd_gw")).thenReturn("s2sAuthKey");
        when(govPayClientMock.retrievePayment(anyString(), anyString())).thenReturn(buildGovPaymentDto());
    }

    @State({"Payments exist for a case"})
    public void toPaymentsForACasesWithSuccess() {
        when(ff4j.check("payment-search")).thenReturn(Boolean.TRUE);
        PaymentFeeLink paymentLink = populateCardPaymentToDb("1", "e2kkddts5215h9qqoeuth5c0v", "ccd_gw").getPaymentLink();
        when(paymentFeeLinkRepositoryMock.findAll(any(Specification.class))).thenReturn(Arrays.asList(paymentLink));

    }

    @State({"A Payment is posted for a case"})
    public void toPostAPaymentWithSuccess() {

        when(govPayKeyRepositoryMock.getKey(null)).thenReturn("govePayKey");
        when(govPayClientMock.createPayment(anyString(), any(CreatePaymentRequest.class))).thenReturn(buildGovPaymentDto());

        PaymentFeeLink paymentLink = populateCardPaymentToDb("1", "e2kkddts5215h9qqoeuth5c0v", "ccd_gw").getPaymentLink();
        when(paymentFeeLinkRepositoryMock.save(null)).thenReturn(paymentLink);

    }

    @State({"The appeal payment amount and fee amount should be equal"})
    public void toPostAppealPaymentWithSuccess() {

        when(govPayKeyRepositoryMock.getKey(null)).thenReturn("govePayKey");
        when(govPayClientMock.createPayment(anyString(), any(CreatePaymentRequest.class))).thenReturn(buildGovPaymentDto());

        PaymentFeeLink paymentLink = populateCardPaymentToDb("1", "e2kkddts5215h9qqoeuth5c0v", "iac").getPaymentLink();
        when(paymentFeeLinkRepositoryMock.save(null)).thenReturn(paymentLink);

    }

    @State({"A request for card payment"})
    public void toPostCardPaymentRequest() {

        when(govPayKeyRepositoryMock.getKey(null)).thenReturn("govePayKey");
        when(govPayClientMock.createPayment(anyString(), any(CreatePaymentRequest.class))).thenReturn(buildGovPaymentDto());

        PaymentFeeLink paymentLink = populateCardPaymentToDb("1", "e2kkddts5215h9qqoeuth5c0v", "iac").getPaymentLink();
        when(paymentFeeLinkRepositoryMock.save(any())).thenReturn(paymentLink);
        when(paymentReferenceMock.getNext()).thenReturn("2018-00000000001");
    }

    private GovPayPayment buildGovPaymentDto() {
        return GovPayPayment.govPaymentWith()
            .amount(10000)
            .state(new uk.gov.hmcts.payment.api.external.client.dto.State("created", false, null, null))
            .description("description")
            .reference("reference")
            .paymentId("paymentId")
            .paymentProvider("sandbox")
            .returnUrl("https://www.google.com")
            .links(GovPayPayment.Links.linksWith().nextUrl(new Link("any", ImmutableMap.of(), "cancelHref", "any")).build())
            .build();

    }

    private Payment populateCardPaymentToDb(String number, String externalReference, String s2sServiceName) {
        //Create a payment in remissionDbBackdoor
        Date now = new Date();
        StatusHistory statusHistory = StatusHistory.statusHistoryWith().status("Initiated").externalStatus("created").build();
        Payment payment = Payment.paymentWith()
            .amount(new BigDecimal("99.99"))
            .caseReference("Reference" + number)
            .ccdCaseNumber("ccdCaseNumber" + number)
            .description("Test payments statuses for " + number)
            .serviceType("Divorce")
            .s2sServiceName(s2sServiceName)
            .currency("GBP")
            .siteId("AA0" + number)
            .userId("USER_ID")
            .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
            .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
            .paymentProvider(PaymentProvider.paymentProviderWith().name("gov pay").build())
            .paymentStatus(PaymentStatus.paymentStatusWith().name("created").build())
            .externalReference(externalReference)
            .reference("654321ABC")
            .status("submitted")
            .statusHistories(Arrays.asList(statusHistory))
            .dateUpdated(now)
            .dateCreated(now)
            .build();

        PaymentFee fee = feeWith().calculatedAmount(new BigDecimal("99.99")).version("1").code("FEE000" + number).volume(1).build();

        PaymentFeeLink paymentFeeLink =
            paymentFeeLinkWith().paymentReference("2018-0000000000" + number).payments(Arrays.asList(payment)).fees(Arrays.asList(fee)).build();
        payment.setPaymentLink(paymentFeeLink);
        return payment;
    }
}
