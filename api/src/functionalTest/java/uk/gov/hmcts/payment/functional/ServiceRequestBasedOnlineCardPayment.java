package uk.gov.hmcts.payment.functional;

import io.restassured.response.Response;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
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
import uk.gov.hmcts.payment.functional.idam.models.User;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;
import uk.gov.hmcts.payment.functional.service.PaymentTestService;
import uk.gov.hmcts.payment.functional.service.ServiceRequestTestService;

import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RunWith(SpringIntegrationSerenityRunner.class)
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
    @Autowired
    private PaymentTestService paymentTestService;
    private static List<String> userEmails = new ArrayList<>();
    private String paymentReference;

    private static String USER_TOKEN_PAYMENT;
    private static String SERVICE_TOKEN;
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
            User user = idamService.createUserWith("payments");
            USER_TOKEN_PAYMENT = user.getAuthorisationToken();
            userEmails.add(user.getEmail());
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);
            TOKENS_INITIALIZED = true;
        }
    }

    @Test
    public void positive_create_service_request_negative_full_card_payment_user_hmcts() throws Exception {

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
    //@Ignore("The initial payment is not Cancelled.")
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

        //First Card Payment
        OnlineCardPaymentRequest onlineCardPaymentRequest = OnlineCardPaymentRequest.onlineCardPaymentRequestWith()
            .amount(new BigDecimal("100.00"))
            .currency(CurrencyCode.GBP)
            .returnUrl("http://localhost.hmcts.net")
            .language("cy")
            .build();
        Response createOnlineCardPaymentResponse =
            serviceRequestTestService.createAnOnlineCardPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
                SERVICE_TOKEN, serviceRequestReference, onlineCardPaymentRequest);
        assertThat(createOnlineCardPaymentResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        OnlineCardPaymentResponse onlineCardPaymentResponse =
            createOnlineCardPaymentResponse.getBody().as(OnlineCardPaymentResponse.class);
        final String initialPaymentReference = onlineCardPaymentResponse.getPaymentReference();
        paymentReference = initialPaymentReference;
        assertThat(initialPaymentReference).matches(PAYMENTS_REGEX_PATTERN);
        assertThat(onlineCardPaymentResponse.getStatus()).isEqualTo("Initiated");

        PaymentDto initialPaymentDto = dsl.given().userToken(USER_TOKEN_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .when().getCardPayment(initialPaymentReference)
            .then().get();

        Response getOnlineCardPaymentResponse =
            serviceRequestTestService.getAnOnlineCardPaymentForAnInternalReference(SERVICE_TOKEN,
                initialPaymentDto.getInternalReference());
        PaymentDto paymentDtoForOnlineCardPaymentDto = getOnlineCardPaymentResponse.getBody().as(PaymentDto.class);
        assertThat(paymentDtoForOnlineCardPaymentDto.getStatus()).isEqualTo("Initiated");
        assertThat(paymentDtoForOnlineCardPaymentDto.getReference()).isEqualTo(initialPaymentReference);

        OnlineCardPaymentRequest onlineCardPaymentRequestAgain = OnlineCardPaymentRequest.onlineCardPaymentRequestWith()
            .amount(new BigDecimal(100.00))
            .currency(CurrencyCode.GBP)
            .returnUrl("http://localhost.hmcts.net")
            .language("cy")
            .build();
        Response createOnlineCardPaymentResponseAgain =
            serviceRequestTestService.createAnOnlineCardPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
                SERVICE_TOKEN, serviceRequestReference, onlineCardPaymentRequestAgain);
        OnlineCardPaymentResponse onlineCardPaymentResponseAgain =
            createOnlineCardPaymentResponseAgain.getBody().as(OnlineCardPaymentResponse.class);
        final String laterPaymentReference = onlineCardPaymentResponseAgain.getPaymentReference();
        assertThat(laterPaymentReference).matches(PAYMENTS_REGEX_PATTERN);
        assertThat(initialPaymentReference).isNotEqualTo(laterPaymentReference);

        Response getOnlineCardPaymentResponseForInitialPaymentResponse =
            serviceRequestTestService.getAnOnlineCardPaymentForAnInternalReference(SERVICE_TOKEN,
                initialPaymentDto.getInternalReference());
        PaymentDto getOnlineCardPaymentInitialDto = getOnlineCardPaymentResponseForInitialPaymentResponse.getBody().as(PaymentDto.class);
        assertThat(getOnlineCardPaymentInitialDto.getStatus()).isEqualTo("Failed");
    }

    @Test
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
            .returnUrl("http://localhost.hmcts.net")
            .language("cy")
            .build();
        Response createOnlineCardPaymentResponse =
            serviceRequestTestService.createAnOnlineCardPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
                SERVICE_TOKEN, serviceRequestReference, onlineCardPaymentRequest);
        assertThat(createOnlineCardPaymentResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        OnlineCardPaymentResponse onlineCardPaymentResponse =
            createOnlineCardPaymentResponse.getBody().as(OnlineCardPaymentResponse.class);
        paymentReference = onlineCardPaymentResponse.getPaymentReference();
        assertThat(paymentReference).matches(PAYMENTS_REGEX_PATTERN);
        assertThat(onlineCardPaymentResponse.getStatus()).isEqualTo("Initiated");
        assertThat(onlineCardPaymentResponse.getNextUrl()).startsWith("https://card.payments.service.gov.uk/secure/");
        assertThat(onlineCardPaymentResponse.getExternalReference()).isNotNull().isNotBlank();

        // Retrieve card payment
        PaymentDto paymentDto = dsl.given().userToken(USER_TOKEN_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .when().getCardPayment(paymentReference)
            .then().get();
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
