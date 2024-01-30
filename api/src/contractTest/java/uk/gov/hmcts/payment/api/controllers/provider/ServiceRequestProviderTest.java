package uk.gov.hmcts.payment.api.controllers.provider;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.payment.api.controllers.ServiceRequestController;
import uk.gov.hmcts.payment.api.domain.service.ServiceRequestDomainService;
import uk.gov.hmcts.payment.api.dto.OnlineCardPaymentRequest;
import uk.gov.hmcts.payment.api.dto.OnlineCardPaymentResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@Provider("payment_accounts")
@PactBroker(scheme = "${PACT_BROKER_SCHEME:http}", host = "${PACT_BROKER_URL:localhost}", port = "${PACT_BROKER_PORT:80}", consumerVersionSelectors = {
    @VersionSelector(tag = "Dev")})
@Import(ServiceRequestProviderTestConfiguration.class)
@IgnoreNoPactsToVerify
class ServiceRequestProviderTest {

    private static final String DATE_TIME_FORMAT_T_HH_MM_SS_SSS = "yyyy-MM-dd'T'HH:mm:ss.SSS";

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
        System.getProperties().setProperty("pact.verifier.publishResults", "true");
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        testTarget.setControllers(
            new ServiceRequestController(serviceRequestDomainServiceMock));
        if (context != null) {
            context.setTarget(testTarget);
        }
    }

    @State({"A Service Request Can be Created for a valid Payload"})
    public void toReturnAccountDetails() throws JSONException, CheckDigitException, ParseException {

        SimpleDateFormat formatter = new SimpleDateFormat(DATE_TIME_FORMAT_T_HH_MM_SS_SSS);
        Date creationDate = formatter.parse("2022-09-05T11:09:04.308");

        OnlineCardPaymentResponse onlineCardPaymentResponse
            = OnlineCardPaymentResponse.onlineCardPaymentResponseWith()
            .externalReference("csfopuk3a6r0e405cqtl9ef5br")
            .nextUrl("https://www.payments.service.gov.uk/secure/3790460a-5932-4364-bba1-75390b4ec758")
            .paymentReference("RC-1662-3761-4393-1823")
            .status("Initiated")
            .dateCreated(creationDate)
            .build();
        when(serviceRequestDomainServiceMock.create(any(OnlineCardPaymentRequest.class), eq("2022-1662375472431"), eq("https://localhost"), ArgumentMatchers.isNull())).thenReturn(onlineCardPaymentResponse);

    }
}
