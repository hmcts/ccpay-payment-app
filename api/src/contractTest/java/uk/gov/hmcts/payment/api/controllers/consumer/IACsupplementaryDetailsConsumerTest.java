package uk.gov.hmcts.payment.api.controllers.consumer;


import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.annotations.PactFolder;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.payment.api.configuration.RestTemplateConfiguration;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.dto.OrganisationalServiceDto;
import uk.gov.hmcts.payment.api.dto.SupplementaryPaymentDto;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.service.IacService;
import uk.gov.hmcts.payment.api.service.PaymentServiceImpl;
import uk.gov.hmcts.payment.api.service.ReferenceDataServiceImpl;
import uk.gov.hmcts.payment.referencedata.service.SiteServiceImpl;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonArray;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;


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
    protected IacService iacService;

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
            .body("{ccd_case_numbers: [6666661111111111]")
            .headers(HttpHeaders.AUTHORIZATION, SOME_AUTHORIZATION_TOKEN, SERVICE_AUTHORIZATION,
                SOME_SERVICE_AUTHORIZATION_TOKEN)
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .willRespondWith()
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .status(200)
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "retrieveSupplementaryDetailsForSingleExistingCase")
    public void verifyRetrieveOrganisationDetails() throws JSONException {

        MultiValueMap<String, String> header = new LinkedMultiValueMap<>();
        header.put("Authorization", Collections.singletonList(SOME_AUTHORIZATION_TOKEN));
        header.put("ServiceAuthorization", Collections.singletonList(SOME_SERVICE_AUTHORIZATION_TOKEN));
        header.put("content-type", Collections.singletonList(MediaType.APPLICATION_JSON_VALUE));
        given(authTokenGenerator.generate()).willReturn(SOME_SERVICE_AUTHORIZATION_TOKEN);
        PaymentDto paymentDto = new PaymentDto();
        List<PaymentDto> paymentDtos = new ArrayList<>();
        paymentDto.setCcdCaseNumber("6666661111111111");
        paymentDtos.add(0, paymentDto);
        ResponseEntity<SupplementaryPaymentDto> supplementaryInfoResponse = iacService.getIacSupplementaryInfo(paymentDtos, "IAC");
        assertOrganisationalDetails(supplementaryInfoResponse);

    }

    private void assertOrganisationalDetails( ResponseEntity<SupplementaryPaymentDto> supplementaryInfoResponse) {
        assertThat(supplementaryInfoResponse.getStatusCode(), equalTo("200"));
//        assertThat(organisationalDetail.getJurisdiction(), equalTo("jurisdiction"));
//        assertThat(organisationalDetail.getServiceId(), equalTo("437345065"));
//        assertThat(organisationalDetail.getOrgUnit(), equalTo("orgUnit"));
//        assertThat(organisationalDetail.getBusinessArea(), equalTo("businessArea"));
//        assertThat(organisationalDetail.getServiceCode(), equalTo("AA07"));
//        assertThat(organisationalDetail.getServiceShortDescription(), equalTo("DIVORCE"));
//        assertThat(organisationalDetail.getCcdServiceName(), equalTo("ccdServiceName"));
//        assertThat(organisationalDetail.getCcdCaseTypes().get(0), equalTo("Divorce"));
//        assertThat(organisationalDetail.getServiceDescription(), equalTo("DIVORCE"));
    }






    @Pact(provider = "ia_caseAccessApi", consumer = "payment_App")
    RequestResponsePact retrieveSupplementaryDetailsForMultipleExistingCases(PactDslWithProvider builder) {
        // @formatter:off
        return builder
            .given("Supplementary details are requested for multiple, existing cases")
            .uponReceiving("A request for Supplementary Details for a multiple existing cases")
            .path("/supplementary-details")
            .method("POST")
            .body("{ccd_case_numbers: [6666661111111111, 6666662222222222]")
            .headers(HttpHeaders.AUTHORIZATION, SOME_AUTHORIZATION_TOKEN, SERVICE_AUTHORIZATION,
                SOME_SERVICE_AUTHORIZATION_TOKEN)
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .willRespondWith()
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .status(200)
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "retrieveSupplementaryDetailsForMultipleExistingCases")
    public void verifyRetrieveOrganisationDetailsForMultipleCases() throws JSONException {

        MultiValueMap<String, String> header = new LinkedMultiValueMap<>();
        header.put("Authorization", Collections.singletonList(SOME_AUTHORIZATION_TOKEN));
        header.put("ServiceAuthorization", Collections.singletonList(SOME_SERVICE_AUTHORIZATION_TOKEN));
        header.put("content-type", Collections.singletonList(MediaType.APPLICATION_JSON_VALUE));
        given(authTokenGenerator.generate()).willReturn(SOME_SERVICE_AUTHORIZATION_TOKEN);
        PaymentDto paymentDto = new PaymentDto();
        List<PaymentDto> paymentDtos = new ArrayList<>();
        paymentDto.setCcdCaseNumber("6666661111111111");
        paymentDtos.add(0, paymentDto);
        paymentDto.setCcdCaseNumber("6666662222222222");
        paymentDtos.add(1, paymentDto);
        ResponseEntity<SupplementaryPaymentDto> supplementaryInfoResponse = iacService.getIacSupplementaryInfo(paymentDtos, "IAC");
        assertOrganisationalDetailsForMultipleCases(supplementaryInfoResponse);

    }

    private void assertOrganisationalDetailsForMultipleCases( ResponseEntity<SupplementaryPaymentDto> supplementaryInfoResponse) {
        assertThat(supplementaryInfoResponse.getStatusCode(), equalTo("200"));
    }




    @Pact(provider = "ia_caseAccessApi", consumer = "payment_App")
    RequestResponsePact retrieveSupplementaryDetailsForMultipleCasesIncludingAMisssingCase(PactDslWithProvider builder) {
        // @formatter:off
        return builder
            .given("Supplementary details are requested for multiple cases, including a missing case")
            .uponReceiving("A request for Supplementary Details for a multiple existing cases along with one missing case")
            .path("/supplementary-details")
            .method("POST")
            .body("{ccd_case_numbers: [6666661111111111, 6666660000000000]")
            .headers(HttpHeaders.AUTHORIZATION, SOME_AUTHORIZATION_TOKEN, SERVICE_AUTHORIZATION,
                SOME_SERVICE_AUTHORIZATION_TOKEN)
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .willRespondWith()
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .status(200)
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "retrieveSupplementaryDetailsForMultipleCasesIncludingAMisssingCase")
    public void verifyRetrieveOrganisationDetailsForMultipleCasesIncludingAMissingCase() throws JSONException {

        MultiValueMap<String, String> header = new LinkedMultiValueMap<>();
        header.put("Authorization", Collections.singletonList(SOME_AUTHORIZATION_TOKEN));
        header.put("ServiceAuthorization", Collections.singletonList(SOME_SERVICE_AUTHORIZATION_TOKEN));
        header.put("content-type", Collections.singletonList(MediaType.APPLICATION_JSON_VALUE));
        given(authTokenGenerator.generate()).willReturn(SOME_SERVICE_AUTHORIZATION_TOKEN);
        PaymentDto paymentDto = new PaymentDto();
        List<PaymentDto> paymentDtos = new ArrayList<>();
        paymentDto.setCcdCaseNumber("6666661111111111");
        paymentDtos.add(0, paymentDto);
        paymentDto.setCcdCaseNumber("6666660000000000");
        paymentDtos.add(1, paymentDto);
        ResponseEntity<SupplementaryPaymentDto> supplementaryInfoResponse = iacService.getIacSupplementaryInfo(paymentDtos, "IAC");
        assertOrganisationalDetailsForMultipleCasesIncludingAMissingCase(supplementaryInfoResponse);

    }

    private void assertOrganisationalDetailsForMultipleCasesIncludingAMissingCase( ResponseEntity<SupplementaryPaymentDto> supplementaryInfoResponse) {
        assertThat(supplementaryInfoResponse.getStatusCode(), equalTo("200"));
    }






    @Pact(provider = "ia_caseAccessApi", consumer = "payment_App")
    RequestResponsePact retrieveSupplementaryDetailsForAnUnknownCase(PactDslWithProvider builder) {
        // @formatter:off
        return builder
            .given("Supplementary details are requested for an unknown case")
            .uponReceiving("A request for Supplementary Details for an unknown case")
            .path("/supplementary-details")
            .method("POST")
            .body("{ccd_case_numbers: [6666660000000000]")
            .headers(HttpHeaders.AUTHORIZATION, SOME_AUTHORIZATION_TOKEN, SERVICE_AUTHORIZATION,
                SOME_SERVICE_AUTHORIZATION_TOKEN)
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .willRespondWith()
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .status(200)
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "retrieveSupplementaryDetailsForAnUnknownCase")
    public void verifyRetrieveOrganisationDetailsForAnUnknownCase() throws JSONException {

        MultiValueMap<String, String> header = new LinkedMultiValueMap<>();
        header.put("Authorization", Collections.singletonList(SOME_AUTHORIZATION_TOKEN));
        header.put("ServiceAuthorization", Collections.singletonList(SOME_SERVICE_AUTHORIZATION_TOKEN));
        header.put("content-type", Collections.singletonList(MediaType.APPLICATION_JSON_VALUE));
        given(authTokenGenerator.generate()).willReturn(SOME_SERVICE_AUTHORIZATION_TOKEN);
        PaymentDto paymentDto = new PaymentDto();
        List<PaymentDto> paymentDtos = new ArrayList<>();
        paymentDto.setCcdCaseNumber("6666660000000000");
        paymentDtos.add(0, paymentDto);
        ResponseEntity<SupplementaryPaymentDto> supplementaryInfoResponse = iacService.getIacSupplementaryInfo(paymentDtos, "IAC");
        assertOrganisationalDetailsForAnUnknownCase(supplementaryInfoResponse);

    }

    private void assertOrganisationalDetailsForAnUnknownCase( ResponseEntity<SupplementaryPaymentDto> supplementaryInfoResponse) {
        assertThat(supplementaryInfoResponse.getStatusCode(), equalTo("200"));
    }
}
