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
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestPaymentBo;
import uk.gov.hmcts.payment.api.dto.OnlineCardPaymentRequest;
import uk.gov.hmcts.payment.api.dto.OnlineCardPaymentResponse;
import uk.gov.hmcts.payment.api.dto.PaymentGroupResponse;
import uk.gov.hmcts.payment.api.dto.ServiceRequestResponseDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestPaymentDto;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.fixture.ServiceRequestFixture;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;
import uk.gov.hmcts.payment.functional.service.ServiceRequestTestService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Pattern;
import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.payment.functional.idam.IdamService.CMC_CASE_WORKER_GROUP;
import static uk.gov.hmcts.payment.functional.idam.IdamService.CMC_CITIZEN_GROUP;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestContextConfiguration.class)
@ActiveProfiles({"functional-tests", "liberataMock"})
public class ServiceRequestFunctionalTests {

    @Autowired
    private TestConfigProperties testProps;

    @Inject
    private ServiceRequestTestService serviceRequestTestService;

    @Autowired
    private IdamService idamService;

    @Autowired
    private S2sTokenService s2sTokenService;

    private static String USER_TOKEN_PAYMENT;
    private static String SERVICE_TOKEN;
    private static String USER_TOKEN_CMC_CITIZEN;
    private static String USER_TOKEN_CMC_SOLICITOR;
    private static boolean TOKENS_INITIALIZED = false;
    private static final Pattern SERVICE_REQUEST_REGEX_PATTERN =
        Pattern.compile("^(" + LocalDate.now().getYear() + ")-([0-9]{13})$");
    private static final Pattern PAYMENTS_REGEX_PATTERN =
        Pattern.compile("^(RC)-([0-9]{4})-([0-9-]{4})-([0-9-]{4})-([0-9-]{4})$");

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
    public void positive_create_service_request_for_payments_user_hmcts() {

        ServiceRequestDto serviceRequestDto
            = ServiceRequestFixture.buildServiceRequestDTO("ABA6", null);
        Response createServiceRequestResponse
            = serviceRequestTestService.createServiceRequest(USER_TOKEN_PAYMENT, SERVICE_TOKEN,
            serviceRequestDto);
        createServiceRequestResponse.getBody().prettyPrint();
        assertThat(createServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        ServiceRequestResponseDto responseDTO = createServiceRequestResponse.getBody().as(ServiceRequestResponseDto.class);
        assertThat(responseDTO.getServiceRequestReference()).matches(SERVICE_REQUEST_REGEX_PATTERN);

        Response getPaymentGroupResponse = serviceRequestTestService.getPaymentGroups(USER_TOKEN_PAYMENT, SERVICE_TOKEN, serviceRequestDto.getCcdCaseNumber());
        assertThat(createServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        PaymentGroupResponse paymentGroupResponse = getPaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        verifyThePaymentGroupResponse(paymentGroupResponse);
        System.out.println("The value of the Status : "+paymentGroupResponse.getPaymentGroups().get(0).getServiceRequestStatus());

        //TODO - Check that the Service Request's Message is Working on the Topic
    }

    private void verifyThePaymentGroupResponse(PaymentGroupResponse paymentGroupResponse) {
        //verifyThePaymentGroupResponse(paymentGroupResponse);
    }

    @Test
    @Ignore("Test Build")
    public void positive_create_service_request_for_cmc_solicitor_user_professional() {

        ServiceRequestDto serviceRequestDto
            = ServiceRequestFixture.buildServiceRequestDTO("ABA6", null);
        Response createServiceRequestResponse
            = serviceRequestTestService.createServiceRequest(USER_TOKEN_CMC_SOLICITOR, SERVICE_TOKEN,
            serviceRequestDto);
        createServiceRequestResponse.getBody().prettyPrint();
        assertThat(createServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        ServiceRequestResponseDto responseDTO = createServiceRequestResponse.getBody().as(ServiceRequestResponseDto.class);
        assertThat(responseDTO.getServiceRequestReference()).matches(SERVICE_REQUEST_REGEX_PATTERN);
        //TODO - Check that the Service Request's Message is Working on the Topic
    }

    @Test
    @Ignore("Test Build")
    public void positive_create_service_request_citizen_user() {

        ServiceRequestDto serviceRequestDto
            = ServiceRequestFixture.buildServiceRequestDTO("ABA6", null);
        Response createServiceRequestResponse
            = serviceRequestTestService.createServiceRequest(USER_TOKEN_CMC_CITIZEN, SERVICE_TOKEN,
            serviceRequestDto);
        assertThat(createServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        ServiceRequestResponseDto responseDTO = createServiceRequestResponse.getBody().as(ServiceRequestResponseDto.class);
        assertThat(responseDTO.getServiceRequestReference()).matches(SERVICE_REQUEST_REGEX_PATTERN);
    }

    @Test
    @Ignore("TODO - Should Fail for and Errorneous HMCTS Org ID")
    public void negative_create_service_request_service_not_found() {

        ServiceRequestDto serviceRequestDto
            = ServiceRequestFixture.buildServiceRequestDTO("Test", null);
        Response createServiceRequestResponse
            = serviceRequestTestService.createServiceRequest(USER_TOKEN_PAYMENT, SERVICE_TOKEN,
            serviceRequestDto);
        assertThat(createServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(createServiceRequestResponse.getBody().asString())
            .isEqualTo("No Service found for given CaseType or HMCTS Org Id");
    }

    @Test
    @Ignore("Test Build")
    public void positive_multiple_create_service_request_for_cmc_solicitor_user_professional() {

        final String ccd_case_number = ServiceRequestFixture.generateUniqueCCDCaseReferenceNumber();
        //Creating the Service request for the First Time.
        ServiceRequestDto serviceRequestDto
            = ServiceRequestFixture.buildServiceRequestDTO("ABA6", ccd_case_number);
        Response createServiceRequestResponse
            = serviceRequestTestService.createServiceRequest(USER_TOKEN_CMC_SOLICITOR, SERVICE_TOKEN,
            serviceRequestDto);
        createServiceRequestResponse.getBody().prettyPrint();
        assertThat(createServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        ServiceRequestResponseDto responseDTOFirstTime =
            createServiceRequestResponse.getBody().as(ServiceRequestResponseDto.class);
        final String firstServiceRequestReference = responseDTOFirstTime.getServiceRequestReference();
        assertThat(firstServiceRequestReference).matches(SERVICE_REQUEST_REGEX_PATTERN);

        //Creating the Service request for the Second Time.
        Response createServiceRequestResponseForADuplicateResponse
            = serviceRequestTestService.createServiceRequest(USER_TOKEN_CMC_SOLICITOR, SERVICE_TOKEN,
            serviceRequestDto);
        createServiceRequestResponseForADuplicateResponse.getBody().prettyPrint();
        assertThat(createServiceRequestResponseForADuplicateResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        ServiceRequestResponseDto responseDTOForADuplicate =
            createServiceRequestResponseForADuplicateResponse.getBody().as(ServiceRequestResponseDto.class);
        final String secondServiceRequestReference = responseDTOForADuplicate.getServiceRequestReference();
        assertThat(secondServiceRequestReference).matches(SERVICE_REQUEST_REGEX_PATTERN);
        assertThat(firstServiceRequestReference).isNotEqualTo(secondServiceRequestReference);
    }

    @Test
    @Ignore("Test Build")
    public void positive_multiple_fees_for_a_create_service_request() {
        ServiceRequestDto serviceRequestDto
            = ServiceRequestFixture.buildServiceRequestDTOWithMultipleFees("ABA6", null);
        Response createServiceRequestResponse
            = serviceRequestTestService.createServiceRequest(USER_TOKEN_CMC_SOLICITOR, SERVICE_TOKEN,
            serviceRequestDto);
        createServiceRequestResponse.getBody().prettyPrint();
        assertThat(createServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        ServiceRequestResponseDto responseDTO = createServiceRequestResponse.getBody().as(ServiceRequestResponseDto.class);
        assertThat(responseDTO.getServiceRequestReference()).matches(SERVICE_REQUEST_REGEX_PATTERN);
    }

    @Test
    @Ignore("Test Build")
    public void negative_duplicate_fees_for_a_create_service_request() {
        ServiceRequestDto serviceRequestDto
            = ServiceRequestFixture.buildServiceRequestDTOWithDuplicateFees("ABA6", null);
        Response createServiceRequestResponse
            = serviceRequestTestService.createServiceRequest(USER_TOKEN_CMC_SOLICITOR, SERVICE_TOKEN,
            serviceRequestDto);
        createServiceRequestResponse.getBody().prettyPrint();
        assertThat(createServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(createServiceRequestResponse.getBody().asString()).isEqualTo("feeCodeUnique: Fee code cannot be duplicated");
    }

    @Test
    @Ignore("Test Build")
    public void positive_create_service_request_and_a_full_pba_payment_user_hmcts() {

        final ServiceRequestDto serviceRequestDto
            = ServiceRequestFixture.buildServiceRequestDTO("ABA6", null);
        final Response createServiceRequestResponse
            = serviceRequestTestService.createServiceRequest(USER_TOKEN_PAYMENT, SERVICE_TOKEN,
            serviceRequestDto);
        createServiceRequestResponse.getBody().prettyPrint();
        assertThat(createServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        final ServiceRequestResponseDto responseDTO = createServiceRequestResponse.getBody().as(ServiceRequestResponseDto.class);
        final String serviceRequestReference = responseDTO.getServiceRequestReference();
        assertThat(serviceRequestReference).matches(SERVICE_REQUEST_REGEX_PATTERN);

        ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto
            .paymentDtoWith().accountNumber("PBAFUNC12345")
            .amount(BigDecimal.valueOf(100.00))
            .currency("GBP")
            .customerReference("123245677").
                build();
        final Response pbaPaymentServiceRequestResponse
            = serviceRequestTestService.createPBAPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN, ServiceRequestFixture.generateUniqueCCDCaseReferenceNumber(),
            serviceRequestReference, serviceRequestPaymentDto);
        assertThat(pbaPaymentServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    @Test
    @Ignore("Test Build")
    //@Ignore("TODO - The Response Body is not of a proper MIME Type....")
    public void negative_create_service_request_and_an_overpayment_pba_payment_user_hmcts() {

        final ServiceRequestDto serviceRequestDto
            = ServiceRequestFixture.buildServiceRequestDTO("ABA6", null);
        final Response createServiceRequestResponse
            = serviceRequestTestService.createServiceRequest(USER_TOKEN_PAYMENT, SERVICE_TOKEN,
            serviceRequestDto);
        createServiceRequestResponse.getBody().prettyPrint();
        assertThat(createServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        final ServiceRequestResponseDto responseDTO = createServiceRequestResponse.getBody().as(ServiceRequestResponseDto.class);
        final String serviceRequestReference = responseDTO.getServiceRequestReference();
        assertThat(serviceRequestReference).matches(SERVICE_REQUEST_REGEX_PATTERN);

        final ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto
            .paymentDtoWith().accountNumber("PBAFUNC12345")
            .amount(BigDecimal.valueOf(100.01))
            .currency("GBP")
            .customerReference("123245677").
                build();
        final Response pbaPaymentServiceRequestResponse
            = serviceRequestTestService.createPBAPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN, ServiceRequestFixture.generateUniqueCCDCaseReferenceNumber(),
            serviceRequestReference, serviceRequestPaymentDto);
        assertThat(pbaPaymentServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.EXPECTATION_FAILED.value());
        assertThat(pbaPaymentServiceRequestResponse.getBody().asString())
            .isEqualTo("The amount should be equal to serviceRequest balance");
    }

    @Test
    @Ignore("Test Build")
    //@Ignore("TODO - The Response Body is not of a proper MIME Type....")
    public void negative_create_service_request_and_an_underpayment_pba_payment_user_hmcts() {
        final ServiceRequestDto serviceRequestDto
            = ServiceRequestFixture.buildServiceRequestDTO("ABA6", null);
        final Response createServiceRequestResponse
            = serviceRequestTestService.createServiceRequest(USER_TOKEN_PAYMENT, SERVICE_TOKEN,
            serviceRequestDto);
        createServiceRequestResponse.getBody().prettyPrint();
        assertThat(createServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        final ServiceRequestResponseDto responseDTO = createServiceRequestResponse.getBody().as(ServiceRequestResponseDto.class);
        final String serviceRequestReference = responseDTO.getServiceRequestReference();
        assertThat(serviceRequestReference).matches(SERVICE_REQUEST_REGEX_PATTERN);

        final ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto
            .paymentDtoWith().accountNumber("PBAFUNC12345")
            .amount(BigDecimal.valueOf(99.99))
            .currency("GBP")
            .customerReference("123245677").
                build();
        final Response pbaPaymentServiceRequestResponse
            = serviceRequestTestService.createPBAPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN, ServiceRequestFixture.generateUniqueCCDCaseReferenceNumber(),
            serviceRequestReference, serviceRequestPaymentDto);
        assertThat(pbaPaymentServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.EXPECTATION_FAILED.value());
        assertThat(pbaPaymentServiceRequestResponse.getBody().asString())
            .isEqualTo("The amount should be equal to serviceRequest balance");
    }

    @Test
    @Ignore("Test Build")
    public void positive_create_service_request_and_a_pba_payment_and_a_duplicate_payment_for_same_idempotent_key() {

        final ServiceRequestDto serviceRequestDto
            = ServiceRequestFixture.buildServiceRequestDTO("ABA6", null);
        final Response createServiceRequestResponse
            = serviceRequestTestService.createServiceRequest(USER_TOKEN_PAYMENT, SERVICE_TOKEN,
            serviceRequestDto);
        createServiceRequestResponse.getBody().prettyPrint();
        assertThat(createServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        final ServiceRequestResponseDto responseDTO = createServiceRequestResponse.getBody().as(ServiceRequestResponseDto.class);
        final String serviceRequestReference = responseDTO.getServiceRequestReference();
        assertThat(serviceRequestReference).matches(SERVICE_REQUEST_REGEX_PATTERN);

        final String idempotentKey = ServiceRequestFixture.generateUniqueCCDCaseReferenceNumber();

        final ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto
            .paymentDtoWith().accountNumber("PBAFUNC12345")
            .amount(BigDecimal.valueOf(100.00))
            .currency("GBP")
            .customerReference("123245677").
                build();

        final Response pbaPaymentServiceRequestResponse
            = serviceRequestTestService.createPBAPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN, idempotentKey,
            serviceRequestReference, serviceRequestPaymentDto);
        assertThat(pbaPaymentServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        ServiceRequestPaymentBo serviceRequestPaymentBo =
            pbaPaymentServiceRequestResponse.getBody().as(ServiceRequestPaymentBo.class);
        final String paymentReference = serviceRequestPaymentBo.getPaymentReference();
        assertThat(paymentReference).matches(PAYMENTS_REGEX_PATTERN);

        final Response pbaPaymentServiceRequestResponseAgain
            = serviceRequestTestService.createPBAPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN, idempotentKey,
            serviceRequestReference, serviceRequestPaymentDto);
        assertThat(pbaPaymentServiceRequestResponseAgain.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        ServiceRequestPaymentBo serviceRequestPaymentBoAgain =
            pbaPaymentServiceRequestResponseAgain.getBody().as(ServiceRequestPaymentBo.class);
        final String paymentReferenceAgain = serviceRequestPaymentBoAgain.getPaymentReference();
        assertThat(paymentReferenceAgain).matches(PAYMENTS_REGEX_PATTERN);
        assertThat(paymentReference).isEqualTo(paymentReferenceAgain);
    }

    @Test
    @Ignore("Test Build")
    public void positive_create_service_request_and_a_pba_payment_and_a_duplicate_payment_for_a_different_idempotent_Key() {

        final ServiceRequestDto serviceRequestDto
            = ServiceRequestFixture.buildServiceRequestDTO("ABA6", null);
        final Response createServiceRequestResponse
            = serviceRequestTestService.createServiceRequest(USER_TOKEN_PAYMENT, SERVICE_TOKEN,
            serviceRequestDto);
        createServiceRequestResponse.getBody().prettyPrint();
        assertThat(createServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        final ServiceRequestResponseDto responseDTO = createServiceRequestResponse.getBody().as(ServiceRequestResponseDto.class);
        final String serviceRequestReference = responseDTO.getServiceRequestReference();
        assertThat(serviceRequestReference).matches(SERVICE_REQUEST_REGEX_PATTERN);

        final String idempotentKey = ServiceRequestFixture.generateUniqueCCDCaseReferenceNumber();

        final ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto
            .paymentDtoWith().accountNumber("PBAFUNC12345")
            .amount(BigDecimal.valueOf(100.00))
            .currency("GBP")
            .customerReference("123245677").
                build();

        final Response pbaPaymentServiceRequestResponse
            = serviceRequestTestService.createPBAPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN, idempotentKey,
            serviceRequestReference, serviceRequestPaymentDto);
        assertThat(pbaPaymentServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        ServiceRequestPaymentBo serviceRequestPaymentBo =
            pbaPaymentServiceRequestResponse.getBody().as(ServiceRequestPaymentBo.class);
        final String paymentReference = serviceRequestPaymentBo.getPaymentReference();
        assertThat(paymentReference).matches(PAYMENTS_REGEX_PATTERN);

        final Response pbaPaymentServiceRequestResponseAgain
            = serviceRequestTestService.createPBAPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN, ServiceRequestFixture.generateUniqueCCDCaseReferenceNumber(),
            serviceRequestReference, serviceRequestPaymentDto);
        assertThat(pbaPaymentServiceRequestResponseAgain.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED.value());
        assertThat(pbaPaymentServiceRequestResponseAgain.getBody().asString())
            .isEqualTo("The serviceRequest has already been paid");
    }

    @Test
    @Ignore("Test Build")
    public void positive_create_service_request_and_a_duplicate_service_request_post_failed_payment_account_deleted() {

        final ServiceRequestDto serviceRequestDto
            = ServiceRequestFixture.buildServiceRequestDTO("ABA6", null);

        final Response createServiceRequestResponse
            = serviceRequestTestService.createServiceRequest(USER_TOKEN_PAYMENT, SERVICE_TOKEN,
            serviceRequestDto);
        createServiceRequestResponse.getBody().prettyPrint();
        assertThat(createServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        final ServiceRequestResponseDto responseDTO = createServiceRequestResponse.getBody().as(ServiceRequestResponseDto.class);
        final String serviceRequestReference = responseDTO.getServiceRequestReference();
        assertThat(serviceRequestReference).matches(SERVICE_REQUEST_REGEX_PATTERN);

        final String idempotentKey = ServiceRequestFixture.generateUniqueCCDCaseReferenceNumber();

        final ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto
            .paymentDtoWith().accountNumber("PBAFUNC12350")
            .amount(BigDecimal.valueOf(100.00))
            .currency("GBP")
            .customerReference("123245677").
                build();

        final Response pbaPaymentServiceRequestResponse
            = serviceRequestTestService.createPBAPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN, idempotentKey,
            serviceRequestReference, serviceRequestPaymentDto);
        assertThat(pbaPaymentServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.GONE.value());
        ServiceRequestPaymentBo serviceRequestPaymentBo =
            pbaPaymentServiceRequestResponse.getBody().as(ServiceRequestPaymentBo.class);
        final String paymentReference = serviceRequestPaymentBo.getPaymentReference();
        assertThat(paymentReference).matches(PAYMENTS_REGEX_PATTERN);

        final Response createServiceRequestResponseAgain
            = serviceRequestTestService.createServiceRequest(USER_TOKEN_PAYMENT, SERVICE_TOKEN,
            serviceRequestDto);
        createServiceRequestResponseAgain.getBody().prettyPrint();
        assertThat(createServiceRequestResponseAgain.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        final ServiceRequestResponseDto responseDTOAgain =
            createServiceRequestResponseAgain.getBody().as(ServiceRequestResponseDto.class);
        final String serviceRequestReferenceAgain = responseDTOAgain.getServiceRequestReference();
        assertThat(serviceRequestReferenceAgain).matches(SERVICE_REQUEST_REGEX_PATTERN);
        assertThat(serviceRequestReferenceAgain).isNotEqualTo(serviceRequestReference);

    }

    @Test
    @Ignore("Test Build")
    public void positive_create_service_request_and_a_duplicate_service_request_post_failed_payment_account_on_hold() {

        final ServiceRequestDto serviceRequestDto
            = ServiceRequestFixture.buildServiceRequestDTO("ABA6", null);

        final Response createServiceRequestResponse
            = serviceRequestTestService.createServiceRequest(USER_TOKEN_PAYMENT, SERVICE_TOKEN,
            serviceRequestDto);
        createServiceRequestResponse.getBody().prettyPrint();
        assertThat(createServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        final ServiceRequestResponseDto responseDTO = createServiceRequestResponse.getBody().as(ServiceRequestResponseDto.class);
        final String serviceRequestReference = responseDTO.getServiceRequestReference();
        assertThat(serviceRequestReference).matches(SERVICE_REQUEST_REGEX_PATTERN);

        final String idempotentKey = ServiceRequestFixture.generateUniqueCCDCaseReferenceNumber();

        final ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto
            .paymentDtoWith().accountNumber("PBAFUNC12355")
            .amount(BigDecimal.valueOf(100.00))
            .currency("GBP")
            .customerReference("123245677").
                build();

        final Response pbaPaymentServiceRequestResponse
            = serviceRequestTestService.createPBAPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN, idempotentKey,
            serviceRequestReference, serviceRequestPaymentDto);
        assertThat(pbaPaymentServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED.value());
        ServiceRequestPaymentBo serviceRequestPaymentBo =
            pbaPaymentServiceRequestResponse.getBody().as(ServiceRequestPaymentBo.class);
        final String paymentReference = serviceRequestPaymentBo.getPaymentReference();
        assertThat(paymentReference).matches(PAYMENTS_REGEX_PATTERN);

        final Response createServiceRequestResponseAgain
            = serviceRequestTestService.createServiceRequest(USER_TOKEN_PAYMENT, SERVICE_TOKEN,
            serviceRequestDto);
        createServiceRequestResponseAgain.getBody().prettyPrint();
        assertThat(createServiceRequestResponseAgain.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        final ServiceRequestResponseDto responseDTOAgain =
            createServiceRequestResponseAgain.getBody().as(ServiceRequestResponseDto.class);
        final String serviceRequestReferenceAgain = responseDTOAgain.getServiceRequestReference();
        assertThat(serviceRequestReferenceAgain).matches(SERVICE_REQUEST_REGEX_PATTERN);
        assertThat(serviceRequestReferenceAgain).isNotEqualTo(serviceRequestReference);

    }

    @Test
    @Ignore("Test Build")
    public void positive_create_service_request_and_a_duplicate_service_request_post_failed_payment_account_over_limit() {

        final ServiceRequestDto serviceRequestDto
            = ServiceRequestFixture.buildServiceRequestDTOWithFees35000("ABA6", null);

        final Response createServiceRequestResponse
            = serviceRequestTestService.createServiceRequest(USER_TOKEN_PAYMENT, SERVICE_TOKEN,
            serviceRequestDto);
        createServiceRequestResponse.getBody().prettyPrint();
        assertThat(createServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        final ServiceRequestResponseDto responseDTO = createServiceRequestResponse.getBody().as(ServiceRequestResponseDto.class);
        final String serviceRequestReference = responseDTO.getServiceRequestReference();
        assertThat(serviceRequestReference).matches(SERVICE_REQUEST_REGEX_PATTERN);

        final String idempotentKey = ServiceRequestFixture.generateUniqueCCDCaseReferenceNumber();

        final ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto
            .paymentDtoWith().accountNumber("PBAFUNC12345")
            .amount(BigDecimal.valueOf(35000.00))
            .currency("GBP")
            .customerReference("123245677").
                build();

        final Response pbaPaymentServiceRequestResponse
            = serviceRequestTestService.createPBAPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN, idempotentKey,
            serviceRequestReference, serviceRequestPaymentDto);
        assertThat(pbaPaymentServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED.value());
        ServiceRequestPaymentBo serviceRequestPaymentBo =
            pbaPaymentServiceRequestResponse.getBody().as(ServiceRequestPaymentBo.class);
        final String paymentReference = serviceRequestPaymentBo.getPaymentReference();
        assertThat(paymentReference).matches(PAYMENTS_REGEX_PATTERN);

        final Response createServiceRequestResponseAgain
            = serviceRequestTestService.createServiceRequest(USER_TOKEN_PAYMENT, SERVICE_TOKEN,
            serviceRequestDto);
        createServiceRequestResponseAgain.getBody().prettyPrint();
        assertThat(createServiceRequestResponseAgain.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        final ServiceRequestResponseDto responseDTOAgain =
            createServiceRequestResponseAgain.getBody().as(ServiceRequestResponseDto.class);
        final String serviceRequestReferenceAgain = responseDTOAgain.getServiceRequestReference();
        assertThat(serviceRequestReferenceAgain).matches(SERVICE_REQUEST_REGEX_PATTERN);
        assertThat(serviceRequestReferenceAgain).isNotEqualTo(serviceRequestReference);

    }


    @Test
    @Ignore("Test Build")
    public void positive_create_service_request_and_a_full_card_payment_user_hmcts() {

        ServiceRequestDto serviceRequestDto
            = ServiceRequestFixture.buildServiceRequestDTO("ABA6", null);
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
        assertThat(onlineCardPaymentResponse.getPaymentReference()).matches(PAYMENTS_REGEX_PATTERN);
    }

    @Test
    @Ignore("Test Build")
    public void positive_create_service_request_and_an_underpayment_full_card_payment_user_hmcts() {

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
            .amount(new BigDecimal(99.99))
            .currency(CurrencyCode.GBP)
            .language("cy")
            .build();
        Response createOnlineCardPaymentResponse =
            serviceRequestTestService.createAnOnlineCardPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
                SERVICE_TOKEN, serviceRequestReference, onlineCardPaymentRequest);
        assertThat(createOnlineCardPaymentResponse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
    }

    @Test
    @Ignore("Test Build")
    public void positive_create_service_request_and_an_overpayment_full_card_payment_user_hmcts() {

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
            .amount(new BigDecimal(100.01))
            .currency(CurrencyCode.GBP)
            .language("cy")
            .build();
        Response createOnlineCardPaymentResponse =
            serviceRequestTestService.createAnOnlineCardPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
                SERVICE_TOKEN, serviceRequestReference, onlineCardPaymentRequest);
        assertThat(createOnlineCardPaymentResponse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
    }
}
