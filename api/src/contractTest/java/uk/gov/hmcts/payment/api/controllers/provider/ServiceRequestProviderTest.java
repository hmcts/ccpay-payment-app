package uk.gov.hmcts.payment.api.controllers.provider;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.payment.api.controllers.AccountController;
import uk.gov.hmcts.payment.api.controllers.ServiceRequestController;
import uk.gov.hmcts.payment.api.domain.service.ServiceRequestDomainService;
import uk.gov.hmcts.payment.api.service.AccountService;

@ExtendWith(SpringExtension.class)
@Provider("payment_accounts")
@PactBroker(scheme = "${PACT_BROKER_SCHEME:http}", host = "${PACT_BROKER_URL:localhost}", port = "${PACT_BROKER_PORT:80}", consumerVersionSelectors = {
    @VersionSelector(tag = "master")})
@Import(CreditAccountPaymentProviderTestConfiguration.class)
@IgnoreNoPactsToVerify
public class ServiceRequestProviderTest {

    @Value("${PACT_BRANCH_NAME}")
    String branchName;

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
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        testTarget.setControllers(
            new ServiceRequestController());
        if (context != null) {
            context.setTarget(testTarget);
        }
    }
}
