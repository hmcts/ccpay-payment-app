package uk.gov.hmcts.payment.functional;

import io.restassured.response.Response;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.apache.commons.lang3.RandomUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestPaymentBo;
import uk.gov.hmcts.payment.api.dto.CasePaymentRequest;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestFeeDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestPaymentDto;
import uk.gov.hmcts.payment.functional.ccd.CcdService;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.idam.models.User;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;
import uk.gov.hmcts.payment.functional.service.PaymentTestService;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RunWith(SpringIntegrationSerenityRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
public class ServiceRequestPaymentFunctionalTest {

    @Autowired
    private TestConfigProperties testProps;
    @Autowired
    private PaymentsTestDsl dsl;
    @Autowired
    private IdamService idamService;
    @Autowired
    private S2sTokenService s2sTokenService;
    @Autowired
    private CcdService ccdService;
    @Autowired
    private PaymentTestService paymentTestService;
    private static List<String> userEmails = new ArrayList<>();
    private String paymentReference;
    private static String USER_TOKEN_PAYMENT;
    private static String IAC_USER_TOKEN;
    private static String IAC_USER_ID;
    private static String SERVICE_TOKEN;
    private static boolean TOKENS_INITIALIZED = false;

    private static final int CCD_EIGHT_DIGIT_UPPER = 99999999;
    private static final int CCD_EIGHT_DIGIT_LOWER = 10000000;

    final String year = String.valueOf(LocalDateTime.now(DateTimeZone.UTC).getYear());
    // Timestamps like 2026-11-19T19:57:33.529+0000
    final String TIMESTAMP_PLUS0000 = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\+0000";
    // Timestamps like 2026-11-19T19:57:24.888+00:00
    final String TIMESTAMP_PLUS00_00 = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\+00:00";

    @Before
    public void setUp() throws Exception {
        if (!TOKENS_INITIALIZED) {
            User user1 = idamService.createUserWith("payments");
            USER_TOKEN_PAYMENT = user1.getAuthorisationToken();
            userEmails.add(user1.getEmail());

            User user2 = idamService.createUserWith("caseworker","caseworker-ia", "caseworker-ia-legalrep-solicitor" );
            IAC_USER_TOKEN = user2.getAuthorisationToken();
            userEmails.add(user2.getEmail());
            IAC_USER_ID = idamService.getUserDetails(IAC_USER_TOKEN);
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);
            TOKENS_INITIALIZED = true;
        }
    }

    @Test
    public void createAServiceRequestAndMakePBAPaymentPRLService() {
        String ccdCaseNumber = "11116464" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        UUID randomUUID = UUID.randomUUID();
        ServiceRequestDto serviceRequestDto = ServiceRequestDto.serviceRequestDtoWith()
            .hmctsOrgId("ABA5")
            .ccdCaseNumber(ccdCaseNumber)
            .caseReference("abcd-defg-hjik-1234")
            .casePaymentRequest(getCasePaymentRequest())
            .callBackUrl("http://callback.hmcts.net")
            .fees(Arrays.asList(ServiceRequestFeeDto.feeDtoWith()
                .calculatedAmount(BigDecimal.valueOf(263.00))
                .code("FEE0336")
                .version("1")
                .build()))
            .build();
        ServiceRequestPaymentDto paymentDto = ServiceRequestPaymentDto.paymentDtoWith()
            .accountNumber("PBAFUNC12345")
            .amount(BigDecimal.valueOf(263.00))
            .organisationName("TestOrg")
            .currency("GBP")
            .customerReference("123456")
            .idempotencyKey(randomUUID.toString())
            .build();
        dsl.given().userToken(USER_TOKEN_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .when().createServiceRequest(serviceRequestDto)
            .then().gotCreated(Map.class, mapResult -> {
                Object serviceRequestReference = mapResult.get("service_request_reference");
                assertThat(serviceRequestReference).isNotNull();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                dsl.given().userToken(USER_TOKEN_PAYMENT)
                    .s2sToken(SERVICE_TOKEN)
                    .when().createServiceRequestCreditAccountPayment(paymentDto, serviceRequestReference.toString())
                    .then().gotCreated(ServiceRequestPaymentBo.class, paymentBo -> {
                        paymentReference = paymentBo.getPaymentReference();
                        assertThat(paymentBo.getPaymentReference()).isNotNull();
                        assertThat(paymentBo.getStatus()).isEqualToIgnoringCase("success");
                    });
            });
    }

    @Test
    public void createAServiceRequestAndMakePBAPaymentIACService() {
        // IAC service should exist in CCD with surname, so we can make a payment for it
        String ccdCaseNumber = ccdService.createIacDraftCase(IAC_USER_ID, IAC_USER_TOKEN, SERVICE_TOKEN);

        UUID randomUUID = UUID.randomUUID();
        ServiceRequestDto serviceRequestDto = ServiceRequestDto.serviceRequestDtoWith()
            .hmctsOrgId("BFA1")
            .ccdCaseNumber(ccdCaseNumber)
            .caseReference("abcd-defg-hjik-1234")
            .casePaymentRequest(getCasePaymentRequest())
            .callBackUrl("http://callback.hmcts.net")
            .fees(Arrays.asList(ServiceRequestFeeDto.feeDtoWith()
                .calculatedAmount(BigDecimal.valueOf(144.00))
                .code("FEE0238")
                .version("4")
                .build()))
            .build();
        ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto.paymentDtoWith()
            .accountNumber("PBAFUNC12345")
            .amount(BigDecimal.valueOf(144.00))
            .organisationName("TestOrg")
            .currency("GBP")
            .customerReference("123456")
            .idempotencyKey(randomUUID.toString())
            .build();
        dsl.given().userToken(IAC_USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createServiceRequest(serviceRequestDto)
            .then().gotCreated(Map.class, mapResult -> {
                Object serviceRequestReference = mapResult.get("service_request_reference");
                assertThat(serviceRequestReference).isNotNull();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                dsl.given().userToken(USER_TOKEN_PAYMENT)
                    .s2sToken(SERVICE_TOKEN)
                    .when().createServiceRequestCreditAccountPayment(serviceRequestPaymentDto, serviceRequestReference.toString())
                    .then().gotCreated(ServiceRequestPaymentBo.class, paymentBo -> {
                        paymentReference = paymentBo.getPaymentReference();
                        assertThat(paymentBo.getPaymentReference()).isNotNull();
                        assertThat(paymentBo.getStatus()).isEqualToIgnoringCase("success");
                    });
            });

        paymentTestService.getPayments(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentReference)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    // --- Payment ---
                    .body("amount", comparesEqualTo(serviceRequestPaymentDto.getAmount()))
                    .body("currency", equalTo(serviceRequestPaymentDto.getCurrency()))
                    .body("ccd_case_number", equalTo(ccdCaseNumber))
                    .body("case_reference", equalTo(serviceRequestDto.getCaseReference()))
                    // payment_reference format: RC-XXXX-XXXX-XXXX-XXXX
                    .body("payment_reference", matchesPattern("RC-\\d{4}-\\d{4}-\\d{4}-\\d{4}"))
                    .body("channel", equalTo("online"))
                    .body("method", equalTo("payment by account"))
                    .body("status", equalTo("Success"))
                    .body("site_id", equalTo("BFA1"))
                    .body("service_name", equalTo("Immigration and Asylum Appeals"))
                    .body("customer_reference", equalTo(serviceRequestPaymentDto.getCustomerReference()))
                    .body("account_number", equalTo(serviceRequestPaymentDto.getAccountNumber()))
                    .body("organisation_name", equalTo(serviceRequestPaymentDto.getOrganisationName()))
                    // payment_group_reference should start with current year YYYY- format, e.g. 2026- for a payment made in 2026
                    .body("payment_group_reference", matchesPattern(LocalDateTime.now(DateTimeZone.UTC).getYear() + "-\\d{13}"))
                    .body("issue_refund_add_refund_add_remission", equalTo(false))
                    .body("issue_refund", equalTo(false))
                    .body("date_created", matchesPattern(TIMESTAMP_PLUS0000))
                    .body("date_updated", matchesPattern(TIMESTAMP_PLUS0000))

                    // --- Fees ---
                    .body("fees", hasSize(1))
                    .body("fees[0].id", notNullValue())
                    .body("fees[0].code", equalTo(serviceRequestDto.getFees().getFirst().getCode()))
                    .body("fees[0].version", equalTo(serviceRequestDto.getFees().getFirst().getVersion()))
                    .body("fees[0].volume", equalTo(1))
                    .body("fees[0].calculated_amount", comparesEqualTo(serviceRequestDto.getFees().getFirst().getCalculatedAmount()))
                    .body("fees[0].memo_line", equalTo("RECEIPT OF FEES - Tribunal issue other"))
                    .body("fees[0].natural_account_code", equalTo("4481102178"))
                    .body("fees[0].ccd_case_number", equalTo(ccdCaseNumber))
                    .body("fees[0].jurisdiction1", equalTo("tribunal"))
                    .body("fees[0].jurisdiction2", equalTo("immigration and asylum chamber"))
                    .body("fees[0].date_created", matchesPattern(TIMESTAMP_PLUS00_00))
                    .body("fees[0].date_updated", matchesPattern(TIMESTAMP_PLUS00_00))
                    .body("fees[0].issue_refund_add_refund_add_remission", equalTo(false))
                    .body("fees[0].add_remission", equalTo(false))

                    // --- Status histories ---
                    .body("status_histories", hasSize(1))
                    .body("status_histories[0].status", equalTo("success"))
                    .body("status_histories[0].date_created", matchesPattern(TIMESTAMP_PLUS0000))

                    // --- Payment allocation ---
                    .body("payment_allocation", empty());
    }

    @Test
    public void negativeCreateAServiceRequestAndMakePBAPaymentForIACServiceWithNoSurname() {
        // IAC service should exist in CCD with surname, otherwise the payment will fail with 400 error for random case numbers.
        String ccdCaseNumber = "11116464" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        UUID randomUUID = UUID.randomUUID();
        ServiceRequestDto serviceRequestDto = ServiceRequestDto.serviceRequestDtoWith()
            .hmctsOrgId("BFA1")
            .ccdCaseNumber(ccdCaseNumber)
            .caseReference("abcd-defg-hjik-1234")
            .casePaymentRequest(getCasePaymentRequest())
            .callBackUrl("http://callback.hmcts.net")
            .fees(Arrays.asList(ServiceRequestFeeDto.feeDtoWith()
                .calculatedAmount(BigDecimal.valueOf(140.00))
                .code("FEE0238")
                .version("3")
                .build()))
            .build();
        ServiceRequestPaymentDto paymentDto = ServiceRequestPaymentDto.paymentDtoWith()
            .accountNumber("PBAFUNC12345")
            .amount(BigDecimal.valueOf(140.00))
            .organisationName("TestOrg")
            .currency("GBP")
            .customerReference("123456")
            .idempotencyKey(randomUUID.toString())
            .build();

        dsl.given().userToken(IAC_USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createServiceRequest(serviceRequestDto)
            .then().gotCreated(Map.class, mapResult -> {
                Object serviceRequestReference = mapResult.get("service_request_reference");
                assertThat(serviceRequestReference).isNotNull();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Response response = dsl.given().userToken(USER_TOKEN_PAYMENT)
                    .s2sToken(SERVICE_TOKEN)
                    .when().createServiceRequestCreditAccountPayment(paymentDto, serviceRequestReference.toString())
                    .then()
                    .getResponse();

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                assertThat(response.getBody().asString())
                    .isEqualTo(String.format("Error occurred while fetching supplementary info from IAC service for payment reference: %s. Error: IAC response missing surname for payment reference: %s", serviceRequestReference, serviceRequestReference));
            });
    }

    @Test
    public void negativeCreateAServiceRequestWithNoCaseReference() {
        String ccdCaseNumber = "11116464" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        UUID randomUUID = UUID.randomUUID();
        ServiceRequestDto serviceRequestDto = ServiceRequestDto.serviceRequestDtoWith()
            .hmctsOrgId("ABA5")
            .ccdCaseNumber(ccdCaseNumber)
            .caseReference(null)
            .casePaymentRequest(getCasePaymentRequest())
            .callBackUrl("http://callback.hmcts.net")
            .fees(Arrays.asList(ServiceRequestFeeDto.feeDtoWith()
                .calculatedAmount(BigDecimal.valueOf(263.00))
                .code("FEE0336")
                .version("5")
                .build()))
            .build();
        ServiceRequestPaymentDto paymentDto = ServiceRequestPaymentDto.paymentDtoWith()
            .accountNumber("PBAFUNC12345")
            .amount(BigDecimal.valueOf(263.00))
            .organisationName("TestOrg")
            .currency("GBP")
            .customerReference("123456")
            .idempotencyKey(randomUUID.toString())
            .build();

        Response response = dsl.given().userToken(IAC_USER_TOKEN)
            .s2sToken(SERVICE_TOKEN)
            .when().createServiceRequest(serviceRequestDto)
            .then()
            .getResponse();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(response.getBody().asString())
            .isEqualTo("caseReference: Case Reference should not be blank");
    }

    private CasePaymentRequest getCasePaymentRequest() {
        CasePaymentRequest casePaymentRequest = CasePaymentRequest.casePaymentRequestWith()
            .action("action")
            .responsibleParty("party")
            .build();

        return casePaymentRequest;
    }

    @After
    public void deletePayment() {
        if (paymentReference != null) {
            // delete payment record
            paymentTestService.deletePayment(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentReference).then().statusCode(NO_CONTENT.value());
        }
    }

    @AfterClass
    public static void tearDown() {
        if (!userEmails.isEmpty()) {
            // delete idam test user
            userEmails.forEach(IdamService::deleteUser);
        }
    }
}
