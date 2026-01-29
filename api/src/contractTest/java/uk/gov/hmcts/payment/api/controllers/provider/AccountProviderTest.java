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
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.payment.api.controllers.AccountController;
import uk.gov.hmcts.payment.api.controllers.ServiceRequestController;
import uk.gov.hmcts.payment.api.domain.service.ServiceRequestDomainService;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.dto.ServiceRequestResponseDto;
import uk.gov.hmcts.payment.api.service.AccountService;
import uk.gov.hmcts.payment.api.util.AccountStatus;

import java.math.BigDecimal;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@Provider("payment_accounts")
@EnabledIfEnvironmentVariable(named = "PACT_BROKER_URL", matches = ".+")
@PactBroker(scheme = "${PACT_BROKER_SCHEME:http}", host = "${PACT_BROKER_URL:localhost}", port = "${PACT_BROKER_PORT:80}", consumerVersionSelectors = {
    @VersionSelector(tag = "master")})
@Import({CreditAccountPaymentProviderTestConfiguration.class, ServiceRequestProviderTestConfiguration.class})
@IgnoreNoPactsToVerify
class AccountProviderTest {

    private static final String DATE_TIME_FORMAT_T_HH_MM_SS_SSS = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    @Value("${PACT_BRANCH_NAME}")
    String branchName;

    @Autowired
    AccountService accountServiceMock;

    @Autowired
    ServiceRequestDomainService serviceRequestDomainServiceMock;

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
        testTarget.setPrintRequestResponse(true);
        testTarget.setControllers(
            new AccountController(accountServiceMock),
            new ServiceRequestController(serviceRequestDomainServiceMock));
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

    @State({"An account exists with identifier PBA1234"})
    public void toReturnAccountDetails() {

        AccountDto expectedDto = new AccountDto("PBA1234", "accountName", new BigDecimal(100),
            new BigDecimal(100), AccountStatus.ACTIVE, new Date());
        when(accountServiceMock.retrieve("PBA1234")).thenReturn(expectedDto);

    }

    @State({"A Service Request Can be Created for a valid Payload"})
    public void toCreateServiceRequest() {

        ServiceRequestResponseDto serviceRequestResponseDto = ServiceRequestResponseDto.serviceRequestResponseDtoWith()
            .serviceRequestReference("2022-1662375472431")
            .build();

        when(serviceRequestDomainServiceMock.create(any(uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestDto.class), any())).thenReturn(serviceRequestResponseDto);

    }
}
