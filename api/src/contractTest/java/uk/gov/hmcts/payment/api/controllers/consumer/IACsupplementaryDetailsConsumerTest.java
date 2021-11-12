package uk.gov.hmcts.payment.api.controllers.consumer;


import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.payment.api.configuration.RestTemplateConfiguration;
import uk.gov.hmcts.payment.api.service.PaymentServiceImpl;
import uk.gov.hmcts.payment.api.service.ReferenceDataServiceImpl;
import uk.gov.hmcts.payment.referencedata.service.SiteServiceImpl;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import org.springframework.http.HttpHeaders;


@ExtendWith(PactConsumerTestExt.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@PactTestFor(providerName = "referenceData_location", port = "8891")
@PactFolder("pacts")
@SpringBootTest({
    "iac.supplementary.info.url : http://localhost:8891"
})
@ContextConfiguration(classes = {ReferenceDataServiceImpl.class})
@Import(RestTemplateConfiguration.class)
public class IACsupplementaryDetailsConsumerTest {
    public static final String SOME_AUTHORIZATION_TOKEN = "Bearer UserAuthToken";
    public static final String SOME_SERVICE_AUTHORIZATION_TOKEN = "ServiceToken";
    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    @Value("${iac.supplementary.info.url}")
    private String iacSupplementaryInfoUrl;


    @Autowired
    protected PaymentServiceImpl PaymentService;

    @MockBean
    SiteServiceImpl siteService;

    @MockBean
    AuthTokenGenerator authTokenGenerator;

    @Pact(provider = "ia_caseAccessApi", consumer = "payment_App")
    RequestResponsePact retrieveSupplementaryDetailsForSingleExistingCase(PactDslWithProvider builder) {
        // @formatter:off
        return builder
            .given("Supplementary details are requested for a single, existing case")
            .uponReceiving("A request for Supplementary Details for a single existing case")
            .path("/supplementary-details")
            .method("POST")
            .body("{ccd_case_numbers: [1234567890123456,1234567890123457,1234567890123458]")
            .headers(HttpHeaders.AUTHORIZATION, SOME_AUTHORIZATION_TOKEN, SERVICE_AUTHORIZATION,
                SOME_SERVICE_AUTHORIZATION_TOKEN)
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .willRespondWith()
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .status(200)
            .toPact();
    }
}
