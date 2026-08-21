package uk.gov.hmcts.payment.api.controllers.provider;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.controllers.CardPaymentController;
import uk.gov.hmcts.payment.api.controllers.CreditAccountPaymentController;
import uk.gov.hmcts.payment.api.controllers.PaymentReference;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestPaymentBo;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.dto.OrganisationalServiceDto;
import uk.gov.hmcts.payment.api.dto.ServiceRequestResponseDto;
import uk.gov.hmcts.payment.api.dto.mapper.CreditAccountDtoMapper;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.external.client.dto.CardDetails;
import uk.gov.hmcts.payment.api.mapper.CreditAccountPaymentRequestMapper;
import uk.gov.hmcts.payment.api.mapper.PBAStatusErrorMapper;
import uk.gov.hmcts.payment.api.model.*;
import uk.gov.hmcts.payment.api.service.*;
import uk.gov.hmcts.payment.api.util.AccountStatus;
import uk.gov.hmcts.payment.api.util.ServiceRequestCaseUtil;
import uk.gov.hmcts.payment.api.v1.model.ServiceIdSupplier;
import uk.gov.hmcts.payment.api.v1.model.UserIdSupplier;
import uk.gov.hmcts.payment.api.validators.DuplicatePaymentValidator;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.payment.api.model.PaymentFee.feeWith;
import static uk.gov.hmcts.payment.api.model.PaymentFeeLink.paymentFeeLinkWith;

@ExtendWith(SpringExtension.class)
@Provider("payment_creditAccountPayment")
@PactBroker(scheme = "${PACT_BROKER_SCHEME:http}", host = "${PACT_BROKER_URL:localhost}", port = "${PACT_BROKER_PORT:80}", consumerVersionSelectors = {
    @VersionSelector(consumer = "civil_service", tag = "master"),
    @VersionSelector(consumer = "divorce_caseOrchestratorService", tag = "master"),
    @VersionSelector(consumer = "fpl_ccdConfiguration", tag = "master"),
    @VersionSelector(consumer = "fr_caseOrchestratorService", tag = "master"),
    @VersionSelector(consumer = "ia_caseDocumentsApi", tag = "master"),
    @VersionSelector(consumer = "ia_casePaymentsApi", tag = "master"),
    @VersionSelector(consumer = "probate_backOffice", tag = "master")
})

@Import(CreditAccountPaymentProviderTestConfiguration.class)
@IgnoreNoPactsToVerify
class CreditAccountPaymentProviderTest {

    private static final String ACCOUNT_NUMBER_KEY = "accountNumber";
    private static final String ACCOUNT_NAME_KEY = "accountName";
    private static final String AVAILABLE_BALANCE_KEY = "availableBalance";

    @Autowired
    PaymentDtoMapper paymentDtoMapper;
    @Autowired
    CreditAccountPaymentService<PaymentFeeLink, String> creditAccountPaymentService;
    @Autowired
    CreditAccountDtoMapper creditAccountDtoMapper;
    @Autowired
    AccountService<AccountDto, String> accountServiceMock;
    @Autowired
    DuplicatePaymentValidator paymentValidator;
    @Autowired
    FeePayApportionRepository feePayApportionRepository;
    @Autowired
    PaymentFeeRepository paymentFeeRepository;
    @Autowired
    PaymentReference paymentReferenceMock;
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

    @Autowired
    ReferenceDataService referenceDataService;
    @Autowired
    AuthTokenGenerator authTokenGenerator;
    @Autowired
    PaymentService<PaymentFeeLink, String> paymentService;

    @Autowired
    ServiceRequestCaseUtil serviceRequestCaseUtil;

    @Autowired
    DelegatingPaymentService<PaymentFeeLink, String> delegatingPaymentService;

    @Autowired
    CardDetailsService<CardDetails, String> cardDetailsService;

    @Autowired
    PciPalPaymentService pciPalPaymentService;

    @Value("${PACT_BRANCH_NAME:master}")
    String branchName;

    private final static String PAYMENT_CHANNEL_ONLINE = "online";

    private final static String PAYMENT_METHOD = "payment by account";

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }

    @BeforeEach
    void before(PactVerificationContext context) throws Exception {
        System.getProperties().setProperty("pact.verifier.publishResults", Boolean.toString(isMasterBranch()));
        // Set provider version for publishing verification results
        String gitCommit = System.getenv().getOrDefault("GIT_COMMIT", getGitCommitHash());
        System.getProperties().setProperty("pact.provider.version", gitCommit);
        System.getProperties().setProperty("pact.provider.branch", branchName != null ? branchName : "master");

        setUpCivilServiceDefaults();

        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        CardPaymentController cardPaymentController =
            new CardPaymentController(delegatingPaymentService, paymentDtoMapper, cardDetailsService, pciPalPaymentService,
                feePayApportionService, featureToggler, referenceDataService, paymentReferenceMock);
        testTarget.setControllers(
            new CreditAccountPaymentController(creditAccountPaymentService, creditAccountDtoMapper, accountServiceMock, paymentValidator,
                feePayApportionService, featureToggler, pbaStatusErrorMapper, requestMapper, Arrays.asList("CMC"), paymentService,
                referenceDataService, authTokenGenerator, paymentReferenceMock),
            cardPaymentController,
            new CivilServicePactController());
        if (context != null) {
            context.setTarget(testTarget);
        }
    }

    private void setUpCivilServiceDefaults() throws Exception {
        PaymentFeeLink cardPaymentLink = buildCardPaymentLink("RC-1700000000000001");

        PaymentDto createdCardPaymentResponse = PaymentDto.payment2DtoWith()
            .amount(new BigDecimal("100.0"))
            .currency(CurrencyCode.GBP)
            .paymentReference("RC-1700000000000001")
            .reference("reference")
            .status("Initiated")
            .build();

        PaymentDto retrievedCardPaymentResponse = PaymentDto.payment2DtoWith()
            .amount(new BigDecimal("100.0"))
            .currency(CurrencyCode.GBP)
            .paymentReference("RC-1700000000000001")
            .reference("reference")
            .status("Success")
            .build();

        OrganisationalServiceDto organisationalServiceDto = OrganisationalServiceDto.orgServiceDtoWith()
            .serviceCode("site_id")
            .serviceDescription("Civil")
            .ccdCaseTypes(Collections.singletonList("CIVIL"))
            .build();

        when(referenceDataService.getOrganisationalDetail(any(), any(), any())).thenReturn(organisationalServiceDto);
        when(delegatingPaymentService.create(any())).thenReturn(cardPaymentLink);
        when(delegatingPaymentService.retrieve(anyString())).thenReturn(cardPaymentLink);
        when(paymentDtoMapper.toCardPaymentDto(any(PaymentFeeLink.class))).thenReturn(createdCardPaymentResponse);
        when(paymentDtoMapper.toRetrieveCardPaymentResponseDto(any(PaymentFeeLink.class), anyString())).thenReturn(retrievedCardPaymentResponse);
        when(paymentDtoMapper.toPaymentStatusesDto(any(Payment.class))).thenReturn(retrievedCardPaymentResponse);
    }

    private PaymentFeeLink buildCardPaymentLink(String reference) {
        Payment payment = Payment.paymentWith()
            .reference(reference)
            .amount(new BigDecimal("100.0"))
            .currency("GBP")
            .status("created")
            .paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
            .statusHistories(Collections.singletonList(StatusHistory.statusHistoryWith().status("success").build()))
            .build();
        PaymentFeeLink paymentFeeLink = paymentFeeLinkWith()
            .paymentReference("RC-1700000000000001")
            .payments(Collections.singletonList(payment))
            .fees(Collections.emptyList())
            .build();
        payment.setPaymentLink(paymentFeeLink);
        return paymentFeeLink;
    }

    private boolean isMasterBranch() {
        return "master".equalsIgnoreCase(branchName != null ? branchName.trim() : "");
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

    @State({"An active account has sufficient funds for a payment"})
    public void toCreateNewCreditAccountPayment(Map<String, Object> paymentMap) {

        setUpMockInteractions(paymentMap, "Payment Status success", "success", AccountStatus.ACTIVE);
    }


    @State({"An active account has insufficient funds for a payment"})
    public void toRefuseCreditAccountPaymentInusfficientFunds(Map<String, Object> paymentMap) {
        setUpMockInteractions(paymentMap, "Payment Status failed", "failed", AccountStatus.ACTIVE);
    }

    @State({"An on hold account requests a payment"})
    public void toRefuseCreditAccountPaymenOnHold(Map<String, Object> paymentMap) {
        setUpMockInteractions(paymentMap, "Payment Status failed", "failed", AccountStatus.ON_HOLD);
    }


    @State({"A deleted account requests a payment"})
    public void toRefuseCreditAccountPaymenDeleted(Map<String, Object> paymentMap) {
        setUpMockInteractions(paymentMap, "Payment Status failed", "failed", AccountStatus.DELETED);
    }


    private void setUpMockInteractions(Map<String, Object> paymentMap, String s, String success, AccountStatus accountStatus) {
        String accountNumber = (String) paymentMap.get(ACCOUNT_NUMBER_KEY);
        String availableBalance = (String) paymentMap.get(AVAILABLE_BALANCE_KEY);
        String accountName = (String) paymentMap.get(ACCOUNT_NAME_KEY);

        when(userIdSupplierMock.get()).thenReturn("userId");
        when(serviceIdSupplierMock.get()).thenReturn("ccd_gw");
        when(paymentChannelRepositoryMock.findByNameOrThrow(PAYMENT_CHANNEL_ONLINE))
            .thenReturn(PaymentChannel.paymentChannelWith().description("Online Channel").name("online").build());
        when(paymentMethodRepositoryMock.findByNameOrThrow(PAYMENT_METHOD))
            .thenReturn(PaymentMethod.paymentMethodWith().description("Payment by account").name("Payment by account").build());
        when(paymentStatusRepositoryMock.findByNameOrThrow(anyString()))
            .thenReturn(PaymentStatus.paymentStatusWith().description(s).name(success).build());
        when(accountServiceMock.retrieve(accountNumber)).thenReturn(AccountDto.accountDtoWith()
            .accountNumber(accountNumber)
            .accountName(accountName)
            .creditLimit(BigDecimal.valueOf(28879))
            .availableBalance(new BigDecimal(availableBalance))
            .status(accountStatus)
            .build());

        OrganisationalServiceDto organisationalServiceDto = OrganisationalServiceDto.orgServiceDtoWith()
            .serviceCode("AAD7")
            .serviceDescription("Divorce")
            .ccdCaseTypes(Collections.singletonList("DIVORCE"))
            .build();

        when(referenceDataService.getOrganisationalDetail(any(),any(), any())).thenReturn(organisationalServiceDto);

        PaymentFeeLink paymentLink = populateCreditPaymentToDb("1", "e2kkddts5215h9qqoeuth5c0v", "ccd_gw", success, s, accountStatus).getPaymentLink();
        when(serviceRequestCaseUtil.enhanceWithServiceRequestCaseDetails(any(), (Payment) any())).thenReturn(paymentLink);

    }

    private Payment populateCreditPaymentToDb(String number, String externalReference, String s2sServiceName, String success, String desc, AccountStatus accountStatus) {

        String errorCode = null;
        String errorMessage = null;
        if (AccountStatus.ACTIVE.equals(accountStatus)) {
            errorCode = "CA-E0001";
            errorMessage = "Payment request failed . PBA account CAERPHILLY COUNTY BOROUGH COUNCIL have insufficient funds available";
        } else if (AccountStatus.ON_HOLD.equals(accountStatus)) {
            errorCode = "CA-E0003";
            errorMessage = "Your account is on hold";
        } else if (AccountStatus.DELETED.equals(accountStatus)) {
            errorCode = "CA-E0004";
            errorMessage = "Your account is deleted";
        }

        //Create a payment in remissionDbBackdoor
        Date now = new Date();
        StatusHistory statusHistory = StatusHistory.statusHistoryWith().message(errorMessage).status(success).externalStatus(success).errorCode(errorCode).build();
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
                .paymentMethod(PaymentMethod.paymentMethodWith().name("Payment by account").build())
                .paymentProvider(PaymentProvider.paymentProviderWith().name("gov pay").build())
                .paymentStatus(PaymentStatus.paymentStatusWith().name(success).description(desc).build())
                .externalReference(externalReference)
                .reference("RC-1700000000000001")
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

    @RestController
    static class CivilServicePactController {
        @PostMapping(value = "/service-request")
        public ResponseEntity<ServiceRequestResponseDto> createServiceRequest(@RequestBody(required = false) Object ignoredRequest) {
            return new ResponseEntity<>(
                ServiceRequestResponseDto.serviceRequestResponseDtoWith()
                    .serviceRequestReference("2026-1700000000000001")
                    .build(),
                HttpStatus.CREATED
            );
        }

        @PostMapping(value = "/service-request/{service-request-reference}/pba-payments")
        public ResponseEntity<ServiceRequestPaymentBo> createServiceRequestPbaPayment(
            @PathVariable("service-request-reference") String ignoredReference,
            @RequestBody(required = false) Object ignoredRequest) {
            return new ResponseEntity<>(
                ServiceRequestPaymentBo.serviceRequestPaymentBoWith()
                    .paymentReference("RC-1700000000000001")
                    .status("Success")
                    .dateCreated("2026-08-14T10:15:30.000Z")
                    .build(),
                HttpStatus.CREATED
            );
        }

        @PostMapping(value = "/service-request/{service-request-reference}/card-payments")
        public ResponseEntity<Map<String, Object>> createServiceRequestCardPayment(
            @PathVariable("service-request-reference") String ignoredReference,
            @RequestBody(required = false) Object ignoredRequest) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("payment_reference", "RC-1700000000000001");
            response.put("external_reference", "external-reference");
            response.put("status", "Initiated");
            response.put("next_url", "https://payments/next");
            response.put("date_created", "2026-08-14T10:15:30Z");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        }
    }

}
