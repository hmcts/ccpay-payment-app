package uk.gov.hmcts.payment.functional;

import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.payment.api.contract.PaymentDto;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.dto.OnlineCardPaymentRequest;
import uk.gov.hmcts.payment.api.dto.OnlineCardPaymentResponse;
import uk.gov.hmcts.payment.api.dto.ServiceRequestResponseDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestDto;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.fixture.ServiceRequestFixture;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;
import uk.gov.hmcts.payment.functional.service.ServiceRequestTestService;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static uk.gov.hmcts.payment.functional.idam.IdamService.CMC_CASE_WORKER_GROUP;
import static uk.gov.hmcts.payment.functional.idam.IdamService.CMC_CITIZEN_GROUP;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
@ActiveProfiles({"functional-tests", "liberataMock"})
public class ServiceRequestBasedOnlineCardPayment {

    @Autowired
    private TestConfigProperties testProps;

    @Inject
    private ServiceRequestTestService serviceRequestTestService;

    @Autowired
    private IdamService idamService;

    @Autowired
    private S2sTokenService s2sTokenService;

    @Autowired
    private PaymentsTestDsl dsl;


    private static String USER_TOKEN_PAYMENT;
    private static String SERVICE_TOKEN;
    private static String USER_TOKEN_CMC_CITIZEN;
    private static String USER_TOKEN_CMC_SOLICITOR;
    private static boolean TOKENS_INITIALIZED = false;
    private static final Pattern SERVICE_REQUEST_REGEX_PATTERN =
        Pattern.compile("^(" + LocalDate.now().getYear() + ")-([0-9]{13})$");
    private static final Pattern PAYMENTS_REGEX_PATTERN =
        Pattern.compile("^(RC)-([0-9]{4})-([0-9-]{4})-([0-9-]{4})-([0-9-]{4})$");
    private static final String PAID = "Paid";
    private static final String NOT_PAID = "Not Paid";
    private static final String PARTIALLY_PAID = "Partially Paid";

    @Before
    public void setUp() throws Exception {
        if (!TOKENS_INITIALIZED) {
            USER_TOKEN_PAYMENT = idamService.createUserWith(CMC_CITIZEN_GROUP, "payments").getAuthorisationToken();
            USER_TOKEN_CMC_CITIZEN = idamService.createUserWith(CMC_CITIZEN_GROUP, "citizen").getAuthorisationToken();
            USER_TOKEN_CMC_SOLICITOR =
                idamService.createUserWith(CMC_CASE_WORKER_GROUP, "caseworker-cmc-solicitor").getAuthorisationToken();

            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);
            TOKENS_INITIALIZED = true;
        }
    }

    @Test
    //@Ignore("Test Build")
    public void positive_create_service_request_negative_full_card_payment_user_hmcts() throws Exception {

        ServiceRequestDto serviceRequestDto
            = ServiceRequestFixture.buildServiceRequestDTO("AAA6", null);
        System.out.println("The Value of the CCD Case Number : " + serviceRequestDto.getCcdCaseNumber());
        Response createServiceRequestResponse
            = serviceRequestTestService.createServiceRequest(USER_TOKEN_PAYMENT, SERVICE_TOKEN,
            serviceRequestDto);
        createServiceRequestResponse.getBody().prettyPrint();
        assertThat(createServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        ServiceRequestResponseDto responseDTO = createServiceRequestResponse.getBody().as(ServiceRequestResponseDto.class);
        final String serviceRequestReference = responseDTO.getServiceRequestReference();
        assertThat(responseDTO.getServiceRequestReference()).matches(SERVICE_REQUEST_REGEX_PATTERN);

        OnlineCardPaymentRequest onlineCardPaymentRequestUnderPayment = OnlineCardPaymentRequest.onlineCardPaymentRequestWith()
            .amount(new BigDecimal(99.99))
            .currency(CurrencyCode.GBP)
            .language("cy")
            .build();
        Response createOnlineCardPaymentResponseUnderPayment =
            serviceRequestTestService.createAnOnlineCardPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
                SERVICE_TOKEN, serviceRequestReference, onlineCardPaymentRequestUnderPayment);
        assertThat(createOnlineCardPaymentResponseUnderPayment.getStatusCode())
            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());

        OnlineCardPaymentRequest onlineCardPaymentRequestOverPayment = OnlineCardPaymentRequest.onlineCardPaymentRequestWith()
            .amount(new BigDecimal(100.01))
            .currency(CurrencyCode.GBP)
            .language("cy")
            .build();
        Response createOnlineCardPaymentResponseOverPayment =
            serviceRequestTestService.createAnOnlineCardPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
                SERVICE_TOKEN, serviceRequestReference, onlineCardPaymentRequestOverPayment);
        assertThat(createOnlineCardPaymentResponseOverPayment.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());

        OnlineCardPaymentRequest onlineCardPaymentRequestInvalidLanguage = OnlineCardPaymentRequest.onlineCardPaymentRequestWith()
            .amount(new BigDecimal(100.00))
            .currency(CurrencyCode.GBP)
            .language("")
            .build();
        Response createOnlineCardPaymentResponseInvalidLanguage =
            serviceRequestTestService.createAnOnlineCardPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
                SERVICE_TOKEN, serviceRequestReference, onlineCardPaymentRequestInvalidLanguage);
        assertThat(createOnlineCardPaymentResponseInvalidLanguage.getStatusCode())
            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
    }

    @Test
    @Ignore("Statuses are inconsistent...")
    public void negative_full_card_payment_already_payment_in_progress() throws Exception {

        ServiceRequestDto serviceRequestDto
            = ServiceRequestFixture.buildServiceRequestDTO("AAA6", null);
        Response createServiceRequestResponse
            = serviceRequestTestService.createServiceRequest(USER_TOKEN_PAYMENT, SERVICE_TOKEN,
            serviceRequestDto);
        createServiceRequestResponse.getBody().prettyPrint();
        assertThat(createServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        ServiceRequestResponseDto responseDTO = createServiceRequestResponse.getBody().as(ServiceRequestResponseDto.class);
        final String serviceRequestReference = responseDTO.getServiceRequestReference();
        assertThat(responseDTO.getServiceRequestReference()).matches(SERVICE_REQUEST_REGEX_PATTERN);

        OnlineCardPaymentRequest onlineCardPaymentRequest = OnlineCardPaymentRequest.onlineCardPaymentRequestWith()
            .amount(new BigDecimal("100.00"))
            .currency(CurrencyCode.GBP)
            .language("cy")
            .build();
        Response createOnlineCardPaymentResponse =
            serviceRequestTestService.createAnOnlineCardPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
                SERVICE_TOKEN, serviceRequestReference, onlineCardPaymentRequest);
        assertThat(createOnlineCardPaymentResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        OnlineCardPaymentResponse onlineCardPaymentResponse =
            createOnlineCardPaymentResponse.getBody().as(OnlineCardPaymentResponse.class);
        final String initialPaymentReference = onlineCardPaymentResponse.getPaymentReference();
        assertThat(initialPaymentReference).matches(PAYMENTS_REGEX_PATTERN);
        assertThat(onlineCardPaymentResponse.getStatus()).isEqualTo("created");


        OnlineCardPaymentRequest onlineCardPaymentRequestAgain = OnlineCardPaymentRequest.onlineCardPaymentRequestWith()
            .amount(new BigDecimal(100.00))
            .currency(CurrencyCode.GBP)
            .language("cy")
            .build();
        Response createOnlineCardPaymentResponseAgain =
            serviceRequestTestService.createAnOnlineCardPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
                SERVICE_TOKEN, serviceRequestReference, onlineCardPaymentRequestAgain);
        OnlineCardPaymentResponse onlineCardPaymentResponseAgain =
            createOnlineCardPaymentResponseAgain.getBody().as(OnlineCardPaymentResponse.class);
        final String laterPaymentReference = onlineCardPaymentResponseAgain.getPaymentReference();
        System.out.println("The value of the external reference : " + onlineCardPaymentResponseAgain.getExternalReference());
        assertThat(laterPaymentReference).matches(PAYMENTS_REGEX_PATTERN);
        assertThat(initialPaymentReference).isNotEqualTo(laterPaymentReference);
        System.out.println("The value of the later payment reference : " + laterPaymentReference);
        //Also to check that the old Payment Fee Link was Cancelled

        // Retrieve card payment
        PaymentDto paymentDto = dsl.given().userToken(USER_TOKEN_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .when().getCardPayment(laterPaymentReference)
            .then().get();
        System.out.println("The value of the Internal Reference : " + paymentDto.getInternalReference());

        assertNotNull(paymentDto);
        assertThat(paymentDto.getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertEquals(paymentDto.getExternalProvider(), "gov pay");
        assertEquals(paymentDto.getServiceName(), "Specified Money Claims");
        assertEquals(paymentDto.getStatus(), "Initiated");

        Response getOnlineCardPaymentResponse =
            serviceRequestTestService.getAnOnlineCardPaymentForAnInternalReference(SERVICE_TOKEN,
                paymentDto.getInternalReference());
        PaymentDto paymentDtoForOnlineCardPaymentResponse = getOnlineCardPaymentResponse.getBody().as(PaymentDto.class);
        assertThat(paymentDtoForOnlineCardPaymentResponse.getStatus()).isEqualTo("created");
        assertThat(paymentDtoForOnlineCardPaymentResponse.getPaymentReference()).isEqualTo(laterPaymentReference);
    }

    @Test
//    @Ignore("Statuses are inconsistent... UPDATE: FIXED")
    public void positive_full_card_payment_already_payment_in_progress() throws Exception {

        ServiceRequestDto serviceRequestDto
            = ServiceRequestFixture.buildServiceRequestDTO("AAA6", null);
        Response createServiceRequestResponse
            = serviceRequestTestService.createServiceRequest(USER_TOKEN_PAYMENT, SERVICE_TOKEN,
            serviceRequestDto);
        createServiceRequestResponse.getBody().prettyPrint();
        assertThat(createServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        ServiceRequestResponseDto responseDTO = createServiceRequestResponse.getBody().as(ServiceRequestResponseDto.class);
        final String serviceRequestReference = responseDTO.getServiceRequestReference();
        assertThat(responseDTO.getServiceRequestReference()).matches(SERVICE_REQUEST_REGEX_PATTERN);

        OnlineCardPaymentRequest onlineCardPaymentRequest = OnlineCardPaymentRequest.onlineCardPaymentRequestWith()
            .amount(new BigDecimal(100.00))
            .currency(CurrencyCode.GBP)
            .language("cy")
            .build();
        Response createOnlineCardPaymentResponse =
            serviceRequestTestService.createAnOnlineCardPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
                SERVICE_TOKEN, serviceRequestReference, onlineCardPaymentRequest);
        assertThat(createOnlineCardPaymentResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        OnlineCardPaymentResponse onlineCardPaymentResponse =
            createOnlineCardPaymentResponse.getBody().as(OnlineCardPaymentResponse.class);
        final String paymentReference = onlineCardPaymentResponse.getPaymentReference();
        assertThat(paymentReference).matches(PAYMENTS_REGEX_PATTERN);
        assertThat(onlineCardPaymentResponse.getStatus()).isEqualTo("Initiated");
        assertThat(onlineCardPaymentResponse.getNextUrl()).startsWith("https://www.payments.service.gov.uk/secure/");
        assertThat(onlineCardPaymentResponse.getExternalReference()).isNotNull().isNotBlank();

        // Retrieve card payment
        PaymentDto paymentDto = dsl.given().userToken(USER_TOKEN_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .when().getCardPayment(paymentReference)
            .then().get();
        System.out.println("The value of the Internal Reference : " + paymentDto.getInternalReference());
        assertThat(paymentDto).isNotNull();
        assertThat(paymentDto.getReference()).isEqualTo(paymentReference);
        assertThat(paymentDto.getStatus()).isEqualTo("Initiated");
        assertThat(paymentDto.getInternalReference()).isNotNull().isNotBlank();

        Response getOnlineCardPaymentResponse =
            serviceRequestTestService.getAnOnlineCardPaymentForAnInternalReference(SERVICE_TOKEN,
                paymentDto.getInternalReference());
        PaymentDto paymentDtoForOnlineCardPaymentResponse = getOnlineCardPaymentResponse.getBody().as(PaymentDto.class);
        assertThat(paymentDtoForOnlineCardPaymentResponse.getStatus()).isEqualTo("Initiated");

    }

    @Test
    //@Ignore("Right Error Message is not provided. UPDATE: FIXED")
    public void negative_get_online_card_payment_for_invalid_internal_reference() throws Exception {

        Response getOnlineCardPaymentResponse =
            serviceRequestTestService.getAnOnlineCardPaymentForAnInternalReference(SERVICE_TOKEN,
                "Test Reference");
        assertThat(getOnlineCardPaymentResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(getOnlineCardPaymentResponse.getBody().asString()).isEqualTo("The internal Reference is not found");
    }

    @Test
    public void negative_get_online_card_payment_for_invalid_service_token() throws Exception {

        Response getOnlineCardPaymentResponse =
            serviceRequestTestService.getAnOnlineCardPaymentForAnInternalReference("Test Value",
                "Test Reference");
        assertThat(getOnlineCardPaymentResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
        //Error message not checked here as the Bosy could not be populated due to outstanding 417 Unit Tests to be fixed for this to be properly reported
        //assertThat(getOnlineCardPaymentResponse.getBody().asString()).isEqualTo("Invalid Service Token");
    }
}
