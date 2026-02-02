package uk.gov.hmcts.payment.functional;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.TopicClient;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import io.restassured.response.Response;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.apache.commons.lang3.RandomUtils;
import org.joda.time.DateTime;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.payment.api.contract.*;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.domain.model.ServiceRequestPaymentBo;
import uk.gov.hmcts.payment.api.dto.*;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestPaymentDto;
import uk.gov.hmcts.payment.api.model.PaymentChannel;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.service.TelephonySystem;
import uk.gov.hmcts.payment.api.util.PaymentMethodType;
import uk.gov.hmcts.payment.functional.config.TestConfigProperties;
import uk.gov.hmcts.payment.functional.config.ValidUser;
import uk.gov.hmcts.payment.functional.dsl.PaymentsTestDsl;
import uk.gov.hmcts.payment.functional.fixture.PaymentFixture;
import uk.gov.hmcts.payment.functional.fixture.ServiceRequestFixture;
import uk.gov.hmcts.payment.functional.idam.IdamService;
import uk.gov.hmcts.payment.functional.idam.models.User;
import uk.gov.hmcts.payment.functional.s2s.S2sTokenService;
import uk.gov.hmcts.payment.functional.service.CaseTestService;
import uk.gov.hmcts.payment.functional.service.PaymentTestService;
import uk.gov.hmcts.payment.functional.service.ServiceRequestTestService;

import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RunWith(SpringIntegrationSerenityRunner.class)
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

    @Autowired
    private PaymentTestService paymentTestService;

    @Inject
    private CaseTestService cardTestService;

    @Autowired
    private PaymentsTestDsl dsl;

    private static List<String> userEmails = new ArrayList<>();
    private String paymentReference;
    private String paymentFailureReference;
    private static String USER_TOKEN_PAYMENT;
    private static String SERVICE_TOKEN;
    private static String USER_TOKEN_CMC_CITIZEN;
    private static String USER_TOKEN_CMC_SOLICITOR;
    private static String USER_TOKEN_PUI_FINANCE_MANAGER;
    private static String USER_TOKEN_PUI_USER_MANAGER;
    private static String USER_TOKEN_PUI_ORGANISATION_MANAGER;
    private static String USER_TOKEN_PUI_CASE_MANAGER;
    private static boolean TOKENS_INITIALIZED = false;
    private static final Pattern SERVICE_REQUEST_REGEX_PATTERN =
        Pattern.compile("^(" + LocalDate.now().getYear() + ")-([0-9]{13})$");
    private static final Pattern PAYMENTS_REGEX_PATTERN =
        Pattern.compile("^(RC)-([0-9]{4})-([0-9-]{4})-([0-9-]{4})-([0-9-]{4})$");
    private static final String PAID = "Paid";
    private static final String NOT_PAID = "Not paid";
    private static final String PARTIALLY_PAID = "Partially paid";
    private static String USER_TOKEN_CARD_PAYMENT;
    private static final String DISPUTED = "Disputed";

    private static final int CCD_EIGHT_DIGIT_UPPER = 99999999;
    private static final int CCD_EIGHT_DIGIT_LOWER = 10000000;

    @Before
    public void setUp() throws Exception {
        if (!TOKENS_INITIALIZED) {
            User user1 = idamService.createUserWith("payments");
            USER_TOKEN_PAYMENT = user1.getAuthorisationToken();
            userEmails.add(user1.getEmail());
            User user2 = idamService.createUserWith("citizen");
            USER_TOKEN_CMC_CITIZEN = user2.getAuthorisationToken();
            userEmails.add(user2.getEmail());
            ValidUser user3 = idamService.createUserWithRefDataEmailFormat("pui-user-manager");
            USER_TOKEN_PUI_USER_MANAGER = user3.getAuthorisationToken();
            userEmails.add(user3.getEmail());
            ValidUser user4 = idamService.createUserWithRefDataEmailFormat("pui-organisation-manager");
            USER_TOKEN_PUI_ORGANISATION_MANAGER = user4.getAuthorisationToken();
            userEmails.add(user4.getEmail());
            ValidUser user5 = idamService.createUserWithRefDataEmailFormat("pui-finance-manager");
            USER_TOKEN_PUI_FINANCE_MANAGER = user5.getAuthorisationToken();
            userEmails.add(user5.getEmail());
            ValidUser user6 = idamService.createUserWithRefDataEmailFormat("pui-case-manager");
            USER_TOKEN_PUI_CASE_MANAGER = user6.getAuthorisationToken();
            userEmails.add(user6.getEmail());
            User user7 = idamService.createUserWith("caseworker-cmc-solicitor");
            USER_TOKEN_CMC_SOLICITOR = user7.getAuthorisationToken();
            userEmails.add(user7.getEmail());
            User user8 = idamService.createUserWith("citizen");
            USER_TOKEN_CARD_PAYMENT = user8.getAuthorisationToken();
            userEmails.add(user8.getEmail());

            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);

            TOKENS_INITIALIZED = true;
        }
    }

    @Test
    public void positive_create_service_request_for_payments_user_hmcts() throws Exception {

        ServiceRequestDto serviceRequestDto
            = ServiceRequestFixture.buildServiceRequestDTO("ABA6", null);
        Response createServiceRequestResponse
            = serviceRequestTestService.createServiceRequest(USER_TOKEN_PAYMENT, SERVICE_TOKEN,
            serviceRequestDto);
        createServiceRequestResponse.getBody().prettyPrint();
        assertThat(createServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        ServiceRequestResponseDto responseDTO = createServiceRequestResponse.getBody().as(ServiceRequestResponseDto.class);
        assertThat(responseDTO.getServiceRequestReference()).matches(SERVICE_REQUEST_REGEX_PATTERN);

        Response getPaymentGroupResponse =
            serviceRequestTestService.getPaymentGroups(USER_TOKEN_PAYMENT, SERVICE_TOKEN, serviceRequestDto.getCcdCaseNumber());
        assertThat(getPaymentGroupResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        PaymentGroupResponse paymentGroupResponse = getPaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        verifyThePaymentGroupResponseForNoRemisssions(serviceRequestDto, paymentGroupResponse);
    }

    @Test
    public void negative_create_service_request_for_cmc_solicitor_user_professional() throws Exception {

        ServiceRequestDto serviceRequestDto
            = ServiceRequestFixture.buildServiceRequestDTO("ABA6", null);
        Response createServiceRequestResponse
            = serviceRequestTestService.createServiceRequest(USER_TOKEN_CMC_SOLICITOR, SERVICE_TOKEN,
            serviceRequestDto);
        createServiceRequestResponse.getBody().prettyPrint();
        assertThat(createServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        ServiceRequestResponseDto responseDTO = createServiceRequestResponse.getBody().as(ServiceRequestResponseDto.class);
        assertThat(responseDTO.getServiceRequestReference()).matches(SERVICE_REQUEST_REGEX_PATTERN);

        Response getPaymentGroupResponse =
            serviceRequestTestService.getPaymentGroups(USER_TOKEN_PAYMENT, SERVICE_TOKEN, serviceRequestDto.getCcdCaseNumber());
        assertThat(getPaymentGroupResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        PaymentGroupResponse paymentGroupResponse = getPaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        verifyThePaymentGroupResponseForNoRemisssions(serviceRequestDto, paymentGroupResponse);

        Response getPaymentGroupResponseForPUIFinanceManager =
            serviceRequestTestService
                .getPaymentGroups(USER_TOKEN_PUI_FINANCE_MANAGER, SERVICE_TOKEN, serviceRequestDto.getCcdCaseNumber());
        assertThat(getPaymentGroupResponseForPUIFinanceManager.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        PaymentGroupResponse getPaymentGroupResponseForPUIFinanceManagerResponse =
            getPaymentGroupResponseForPUIFinanceManager.getBody().as(PaymentGroupResponse.class);
        verifyThePaymentGroupResponseForNoRemisssions(serviceRequestDto,
            getPaymentGroupResponseForPUIFinanceManagerResponse);

        Response getPaymentGroupResponseForPUICaseManager =
            serviceRequestTestService
                .getPaymentGroups(USER_TOKEN_PUI_CASE_MANAGER, SERVICE_TOKEN, serviceRequestDto.getCcdCaseNumber());
        assertThat(getPaymentGroupResponseForPUICaseManager.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        PaymentGroupResponse getPaymentGroupResponseForPUICaseManagerResponse =
            getPaymentGroupResponseForPUICaseManager.getBody().as(PaymentGroupResponse.class);
        verifyThePaymentGroupResponseForNoRemisssions(serviceRequestDto,
            getPaymentGroupResponseForPUICaseManagerResponse);

        Response getPaymentGroupResponseForPUIOrganisationManager =
            serviceRequestTestService
                .getPaymentGroups(USER_TOKEN_PUI_ORGANISATION_MANAGER, SERVICE_TOKEN, serviceRequestDto.getCcdCaseNumber());
        assertThat(getPaymentGroupResponseForPUIOrganisationManager.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        PaymentGroupResponse getPaymentGroupResponseForPUIOrganisationManagerResponse =
            getPaymentGroupResponseForPUIOrganisationManager.getBody().as(PaymentGroupResponse.class);
        verifyThePaymentGroupResponseForNoRemisssions(serviceRequestDto,
            getPaymentGroupResponseForPUIOrganisationManagerResponse);

        Response getPaymentGroupResponseForPUIUserManager =
            serviceRequestTestService
                .getPaymentGroups(USER_TOKEN_PUI_USER_MANAGER, SERVICE_TOKEN, serviceRequestDto.getCcdCaseNumber());
        assertThat(getPaymentGroupResponseForPUIUserManager.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        PaymentGroupResponse getPaymentGroupResponseForPUIUserManagerResponse =
            getPaymentGroupResponseForPUIUserManager.getBody().as(PaymentGroupResponse.class);
        verifyThePaymentGroupResponseForNoRemisssions(serviceRequestDto,
            getPaymentGroupResponseForPUIUserManagerResponse);

        Response getPaymentGroupResponseForCmcSolicitor =
            serviceRequestTestService
                .getPaymentGroups(USER_TOKEN_CMC_SOLICITOR, SERVICE_TOKEN, serviceRequestDto.getCcdCaseNumber());
        assertThat(getPaymentGroupResponseForCmcSolicitor.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(getPaymentGroupResponseForCmcSolicitor.getBody().asString()).isEqualTo("User does not have a valid role");
    }

    @Test
    public void positive_create_service_request_citizen_user() throws Exception {

        ServiceRequestDto serviceRequestDto
            = ServiceRequestFixture.buildServiceRequestDTO("ABA6", null);
        Response createServiceRequestResponse
            = serviceRequestTestService.createServiceRequest(USER_TOKEN_CMC_CITIZEN, SERVICE_TOKEN,
            serviceRequestDto);
        assertThat(createServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        ServiceRequestResponseDto responseDTO = createServiceRequestResponse.getBody().as(ServiceRequestResponseDto.class);
        assertThat(responseDTO.getServiceRequestReference()).matches(SERVICE_REQUEST_REGEX_PATTERN);

        Response getPaymentGroupResponse =
            serviceRequestTestService.getPaymentGroups(USER_TOKEN_PAYMENT, SERVICE_TOKEN, serviceRequestDto.getCcdCaseNumber());
        assertThat(getPaymentGroupResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());

    }

    @Test
    //  No proper Error message for a ccdCaseNumber that is not present
    public void negative_create_service_request_service_not_found() {

        Response getPaymentGroupResponse =
            serviceRequestTestService.getPaymentGroups(USER_TOKEN_PAYMENT, SERVICE_TOKEN, "Unknown");
        assertThat(getPaymentGroupResponse.getStatusCode()).isEqualTo(NO_CONTENT.value());
    }

    @Test
    public void positive_multiple_create_service_request_for_cmc_solicitor_user_professional() throws Exception {

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

        Response getPaymentGroupResponse =
            serviceRequestTestService
                .getPaymentGroups(USER_TOKEN_PUI_USER_MANAGER, SERVICE_TOKEN, serviceRequestDto.getCcdCaseNumber());
        assertThat(getPaymentGroupResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        PaymentGroupResponse paymentGroupResponse = getPaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        verifyThePaymentGroupResponseForNoRemisssions(serviceRequestDto, paymentGroupResponse);
    }

    @Test
    public void positive_multiple_fees_for_a_create_service_request() throws Exception {
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
    public void negative_duplicate_fees_for_a_create_service_request() throws Exception {
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
    public void positive_create_service_request_and_a_full_pba_payment_user_hmcts() throws Exception {

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
            .organisationName("TestOrg")
            .currency("GBP")
            .idempotencyKey(ServiceRequestFixture.generateUniqueCCDCaseReferenceNumber())
            .customerReference("123245677").
                build();
        final Response pbaPaymentServiceRequestResponse
            = serviceRequestTestService.createPBAPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN,
            serviceRequestReference, serviceRequestPaymentDto);
        assertThat(pbaPaymentServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());

        Response getPaymentGroupResponseForPaymentUser =
            serviceRequestTestService.getPaymentGroups(USER_TOKEN_PAYMENT, SERVICE_TOKEN, serviceRequestDto.getCcdCaseNumber());
        assertThat(getPaymentGroupResponseForPaymentUser.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        PaymentGroupResponse paymentGroupResponseForAPaymentUser =
            getPaymentGroupResponseForPaymentUser.getBody().as(PaymentGroupResponse.class);
        verifyThePaymentGroupResponseForUsers(serviceRequestDto, paymentGroupResponseForAPaymentUser, true, true);

        Response getPaymentGroupResponseForSolicitorUser =
            serviceRequestTestService
                .getPaymentGroups(USER_TOKEN_PUI_CASE_MANAGER, SERVICE_TOKEN, serviceRequestDto.getCcdCaseNumber());
        assertThat(getPaymentGroupResponseForSolicitorUser.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        PaymentGroupResponse paymentGroupResponseForASolicitorUser =
            getPaymentGroupResponseForSolicitorUser.getBody().as(PaymentGroupResponse.class);
        verifyThePaymentGroupResponseForUsers(serviceRequestDto, paymentGroupResponseForASolicitorUser, false, true);
    }

    @Test
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
            .organisationName("TestOrg")
            .amount(BigDecimal.valueOf(100.01))
            .currency("GBP")
            .idempotencyKey(ServiceRequestFixture.generateUniqueCCDCaseReferenceNumber())
            .customerReference("123245677").
                build();
        final Response pbaPaymentServiceRequestResponse
            = serviceRequestTestService.createPBAPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN, serviceRequestReference, serviceRequestPaymentDto);
        assertThat(pbaPaymentServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.EXPECTATION_FAILED.value());
        assertThat(pbaPaymentServiceRequestResponse.getBody().asString())
            .isEqualTo("The amount should be equal to serviceRequest balance");
    }

    @Test
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
            .organisationName("TestOrg")
            .currency("GBP")
            .idempotencyKey(ServiceRequestFixture.generateUniqueCCDCaseReferenceNumber())
            .customerReference("123245677").
                build();
        final Response pbaPaymentServiceRequestResponse
            = serviceRequestTestService.createPBAPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN, serviceRequestReference, serviceRequestPaymentDto);
        assertThat(pbaPaymentServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.EXPECTATION_FAILED.value());
        assertThat(pbaPaymentServiceRequestResponse.getBody().asString())
            .isEqualTo("The amount should be equal to serviceRequest balance");
    }

    @Test
    public void positive_create_service_request_and_a_pba_payment_and_a_duplicate_payment_for_same_idempotent_key()
        throws Exception {
        String ccdCaseNumber = "11111234" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        final ServiceRequestDto serviceRequestDto
            = ServiceRequestFixture.buildServiceRequestDTO("ABA6", ccdCaseNumber);
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
            .organisationName("TestOrg")
            .amount(BigDecimal.valueOf(100.00))
            .currency("GBP")
            .idempotencyKey(ServiceRequestFixture.generateUniqueCCDCaseReferenceNumber())
            .customerReference("123245677").
                build();

        final Response pbaPaymentServiceRequestResponse
            = serviceRequestTestService.createPBAPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN,
            serviceRequestReference, serviceRequestPaymentDto);

        assertThat(pbaPaymentServiceRequestResponse.getStatusCode()).isEqualTo(CREATED.value());
        ServiceRequestPaymentBo serviceRequestPaymentBo =
            pbaPaymentServiceRequestResponse.getBody().as(ServiceRequestPaymentBo.class);
        final String paymentReference = serviceRequestPaymentBo.getPaymentReference();
        assertThat(paymentReference).matches(PAYMENTS_REGEX_PATTERN);

        final Response pbaPaymentServiceRequestResponseAgain
            = serviceRequestTestService.createPBAPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN, serviceRequestReference, serviceRequestPaymentDto);
        assertThat(pbaPaymentServiceRequestResponseAgain.getStatusCode()).isEqualTo(CREATED.value());
        assertThat(pbaPaymentServiceRequestResponseAgain.getBody().asString()).isEqualTo(pbaPaymentServiceRequestResponse.getBody().asString());
    }

    @Test
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

        final ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto
            .paymentDtoWith().accountNumber("PBAFUNC12345")
            .amount(BigDecimal.valueOf(100.00))
            .organisationName("TestOrg")
            .currency("GBP")
            .idempotencyKey(ServiceRequestFixture.generateUniqueCCDCaseReferenceNumber())
            .customerReference("123245677").
                build();

        final Response pbaPaymentServiceRequestResponse
            = serviceRequestTestService.createPBAPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN,
            serviceRequestReference, serviceRequestPaymentDto);
        assertThat(pbaPaymentServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());

        ServiceRequestPaymentBo serviceRequestPaymentBo =
            pbaPaymentServiceRequestResponse.getBody().as(ServiceRequestPaymentBo.class);
        paymentReference = serviceRequestPaymentBo.getPaymentReference();
        assertThat(paymentReference).matches(PAYMENTS_REGEX_PATTERN);

        serviceRequestPaymentDto.setIdempotencyKey(ServiceRequestFixture.generateUniqueCCDCaseReferenceNumber());
        final Response pbaPaymentServiceRequestResponseAgain
            = serviceRequestTestService.createPBAPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN,
            serviceRequestReference, serviceRequestPaymentDto);
        assertThat(pbaPaymentServiceRequestResponseAgain.getStatusCode()).isEqualTo(CONFLICT.value());
        assertThat(pbaPaymentServiceRequestResponseAgain.getBody().asString())
            .isEqualTo("Payment already present with conflicting payment details");
    }

    @Test
    public void positive_create_service_request_and_a_duplicate_service_request_post_failed_payment_account_deleted() {
        Assume.assumeTrue(!testProps.baseTestUrl.contains("payment-api-pr-"));

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
            .paymentDtoWith().accountNumber(testProps.deletedAccountNumber)
            .amount(BigDecimal.valueOf(100.00))
            .currency("GBP")
            .organisationName("TestOrg")
            .idempotencyKey(ServiceRequestFixture.generateUniqueCCDCaseReferenceNumber())
            .customerReference("123245677").
                build();

        final Response pbaPaymentServiceRequestResponse
            = serviceRequestTestService.createPBAPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN,
            serviceRequestReference, serviceRequestPaymentDto);
        assertThat(pbaPaymentServiceRequestResponse.getStatusCode()).isEqualTo(GONE.value());

        ServiceRequestPaymentBo serviceRequestPaymentBo =
            pbaPaymentServiceRequestResponse.getBody().as(ServiceRequestPaymentBo.class);
        paymentReference = serviceRequestPaymentBo.getPaymentReference();
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
    public void positive_create_service_request_and_a_duplicate_service_request_post_failed_payment_account_on_hold() {
        Assume.assumeTrue(!testProps.baseTestUrl.contains("payment-api-pr-"));

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
            .paymentDtoWith().accountNumber(testProps.onHoldAccountNumber)
            .amount(BigDecimal.valueOf(100.00))
            .organisationName("TestOrg")
            .currency("GBP")
            .idempotencyKey(ServiceRequestFixture.generateUniqueCCDCaseReferenceNumber())
            .customerReference("123245677").
                build();

        final Response pbaPaymentServiceRequestResponse
            = serviceRequestTestService.createPBAPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN,
            serviceRequestReference, serviceRequestPaymentDto);
        assertThat(pbaPaymentServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED.value());
        ServiceRequestPaymentBo serviceRequestPaymentBo =
            pbaPaymentServiceRequestResponse.getBody().as(ServiceRequestPaymentBo.class);
        paymentReference = serviceRequestPaymentBo.getPaymentReference();
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

        final ServiceRequestPaymentDto serviceRequestPaymentDto = ServiceRequestPaymentDto
            .paymentDtoWith().accountNumber("PBAFUNC12345")
            .amount(BigDecimal.valueOf(35000.00))
            .currency("GBP")
            .organisationName("TestOrg")
            .idempotencyKey(ServiceRequestFixture.generateUniqueCCDCaseReferenceNumber())
            .customerReference("123245677").
                build();

        final Response pbaPaymentServiceRequestResponse
            = serviceRequestTestService.createPBAPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN,
            serviceRequestReference, serviceRequestPaymentDto);
        assertThat(pbaPaymentServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED.value());
        ServiceRequestPaymentBo serviceRequestPaymentBo =
            pbaPaymentServiceRequestResponse.getBody().as(ServiceRequestPaymentBo.class);
        paymentReference = serviceRequestPaymentBo.getPaymentReference();
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
    public void positive_create_service_request_and_a_full_card_payment_user_hmcts() throws Exception {

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
            .returnUrl("https://localhost.hmcts.net")
            .build();
        Response createOnlineCardPaymentResponse =
            serviceRequestTestService.createAnOnlineCardPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
                SERVICE_TOKEN, serviceRequestReference, onlineCardPaymentRequest);
        assertThat(createOnlineCardPaymentResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        OnlineCardPaymentResponse onlineCardPaymentResponse =
            createOnlineCardPaymentResponse.getBody().as(OnlineCardPaymentResponse.class);
        paymentReference = onlineCardPaymentResponse.getPaymentReference();
        assertThat(onlineCardPaymentResponse.getPaymentReference()).matches(PAYMENTS_REGEX_PATTERN);
        assertThat(onlineCardPaymentResponse.getNextUrl()).isNotNull();
        assertThat(onlineCardPaymentResponse.getNextUrl()).isNotBlank();
        assertThat(onlineCardPaymentResponse.getNextUrl()).startsWith("https://");

        Response getPaymentGroupResponseForPaymentUser =
            serviceRequestTestService.getPaymentGroups(USER_TOKEN_PAYMENT, SERVICE_TOKEN, serviceRequestDto.getCcdCaseNumber());
        assertThat(getPaymentGroupResponseForPaymentUser.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        PaymentGroupResponse paymentGroupResponseForAPaymentUser =
            getPaymentGroupResponseForPaymentUser.getBody().as(PaymentGroupResponse.class);
        verifyThePaymentGroupResponseForUsers(serviceRequestDto, paymentGroupResponseForAPaymentUser, true, false);

        Response getPaymentGroupResponseForSolicitorUser =
            serviceRequestTestService
                .getPaymentGroups(USER_TOKEN_PUI_ORGANISATION_MANAGER, SERVICE_TOKEN, serviceRequestDto.getCcdCaseNumber());
        assertThat(getPaymentGroupResponseForSolicitorUser.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        PaymentGroupResponse paymentGroupResponseForASolicitorUser =
            getPaymentGroupResponseForSolicitorUser.getBody().as(PaymentGroupResponse.class);
        verifyThePaymentGroupResponseForNoRemisssions(serviceRequestDto, paymentGroupResponseForASolicitorUser);
    }

    @Test
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

    @Test
    public void negative_get_service_request_for_invalid_ccd_case_number() {
        Response getPaymentGroupResponse =
            serviceRequestTestService.getPaymentGroups(USER_TOKEN_PAYMENT, SERVICE_TOKEN, "Test-Invalid");
        assertThat(getPaymentGroupResponse.getStatusCode()).isEqualTo(NO_CONTENT.value());
    }

    @Test
    public void negative_create_service_request_for_invalid_hmcts_org_id() {
        ServiceRequestDto serviceRequestDto
            = ServiceRequestFixture.buildServiceRequestDTO("ZAM6", null);
        Response createServiceRequestResponse
            = serviceRequestTestService.createServiceRequest(USER_TOKEN_PAYMENT, SERVICE_TOKEN,
            serviceRequestDto);
        assertThat(createServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(createServiceRequestResponse.getBody().asString())
            .isEqualTo("No Service found for given CaseType or HMCTS Org Id");
    }

    @Test
    public void return_disputed_when_failure_event_has_happen_ping_one() {

        String ccdCaseNumber = "11111235" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        FeeDto feeDto = FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("550.00"))
            .ccdCaseNumber(ccdCaseNumber)
            .version("4")
            .code("FEE0002")
            .description("Application for a third party debt order")
            .build();

        TelephonyCardPaymentsRequest telephonyPaymentRequest = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(new BigDecimal("550"))
            .ccdCaseNumber(ccdCaseNumber)
            .currency(CurrencyCode.GBP)
            .caseType("DIVORCE")
            .telephonySystem(TelephonySystem.DEFAULT_SYSTEM_NAME)
            .returnURL("https://www.moneyclaims.service.gov.uk")
            .build();

        PaymentGroupDto groupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Arrays.asList(feeDto)).build();

        dsl.given().userToken(USER_TOKEN_CARD_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(groupDto)
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                assertThat(paymentGroupFeeDto).isNotNull();

                String paymentGroupReference = paymentGroupFeeDto.getPaymentGroupReference();

                dsl.given().userToken(USER_TOKEN_CARD_PAYMENT)
                    .s2sToken(SERVICE_TOKEN)
                    .returnUrl("https://www.moneyclaims.service.gov.uk")
                    .when().createTelephonyPayment(telephonyPaymentRequest, paymentGroupReference)
                    .then().gotCreated(TelephonyCardPaymentsResponse.class, telephonyCardPaymentsResponse -> {
                        assertThat(telephonyCardPaymentsResponse).isNotNull();
                        assertThat(telephonyCardPaymentsResponse.getStatus()).isEqualTo("Initiated");
                        paymentReference = telephonyCardPaymentsResponse.getPaymentReference();
                    });
                // pci-pal callback
                TelephonyCallbackDto callbackDto = TelephonyCallbackDto.telephonyCallbackWith()
                    .orderReference(paymentReference)
                    .orderAmount("550")
                    .transactionResult("SUCCESS")
                    .build();

                dsl.given().s2sToken(SERVICE_TOKEN)
                    .when().telephonyCallback(callbackDto)
                    .then().noContent();

            });


        Response casePaymentGroupResponse
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN,ccdCaseNumber );
        PaymentGroupResponse paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        assertThat(paymentGroupResponse.getPaymentGroups().get(0).getServiceRequestStatus())
            .isEqualTo(PAID);

        PaymentStatusChargebackDto paymentStatusChargebackDto
            = PaymentFixture.chargebackRequestService(paymentReference,ccdCaseNumber);

        paymentFailureReference = paymentStatusChargebackDto.getFailureReference();
        Response chargebackResponse = paymentTestService.postChargeback(
            USER_TOKEN_PAYMENT,
            paymentStatusChargebackDto);

        PaymentFailureResponse paymentsFailureResponse =
            paymentTestService.getFailurePayment(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentStatusChargebackDto.getPaymentReference()).then()
                .statusCode(OK.value()).extract().as(PaymentFailureResponse.class);

        assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getFailureReference()).isEqualTo(paymentStatusChargebackDto.getFailureReference());

        assertThat(chargebackResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        Response casePaymentGroupResponse1
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN, ccdCaseNumber);

        PaymentGroupResponse paymentGroupResponse1
            = casePaymentGroupResponse1.getBody().as(PaymentGroupResponse.class);

        assertThat(paymentGroupResponse1.getPaymentGroups().get(0).getServiceRequestStatus())
            .isEqualTo(DISPUTED);
    }

    @Test
    public void return_paid_when_failure_event_and_HMCTS_won_dispute_ping_two() {
        String ccdCaseNumber = "11111234" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        FeeDto feeDto = FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("550.00"))
            .ccdCaseNumber(ccdCaseNumber)
            .version("4")
            .code("FEE0002")
            .description("Application for a third party debt order")
            .build();

        TelephonyCardPaymentsRequest telephonyPaymentRequest = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(new BigDecimal("550"))
            .ccdCaseNumber(ccdCaseNumber)
            .currency(CurrencyCode.GBP)
            .caseType("DIVORCE")
             .telephonySystem(TelephonySystem.DEFAULT_SYSTEM_NAME)
            .returnURL("https://www.moneyclaims.service.gov.uk")
            .build();

        PaymentGroupDto groupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Arrays.asList(feeDto)).build();

        dsl.given().userToken(USER_TOKEN_CARD_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(groupDto)
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                assertThat(paymentGroupFeeDto).isNotNull();

                String paymentGroupReference = paymentGroupFeeDto.getPaymentGroupReference();

                dsl.given().userToken(USER_TOKEN_CARD_PAYMENT)
                    .s2sToken(SERVICE_TOKEN)
                    .returnUrl("https://www.moneyclaims.service.gov.uk")
                    .when().createTelephonyPayment(telephonyPaymentRequest, paymentGroupReference)
                    .then().gotCreated(TelephonyCardPaymentsResponse.class, telephonyCardPaymentsResponse -> {
                        assertThat(telephonyCardPaymentsResponse).isNotNull();
                        assertThat(telephonyCardPaymentsResponse.getStatus()).isEqualTo("Initiated");
                        paymentReference = telephonyCardPaymentsResponse.getPaymentReference();
                    });
                // pci-pal callback
                TelephonyCallbackDto callbackDto = TelephonyCallbackDto.telephonyCallbackWith()
                    .orderReference(paymentReference)
                    .orderAmount("550")
                    .transactionResult("SUCCESS")
                    .build();

                dsl.given().s2sToken(SERVICE_TOKEN)
                    .when().telephonyCallback(callbackDto)
                    .then().noContent();

            });
        Response casePaymentGroupResponse
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN, ccdCaseNumber);
        PaymentGroupResponse paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        assertThat(paymentGroupResponse.getPaymentGroups().get(0).getServiceRequestStatus())
            .isEqualTo(PAID);

        PaymentStatusChargebackDto paymentStatusChargebackDto
            = PaymentFixture.chargebackRequestService(paymentReference,ccdCaseNumber);
        paymentFailureReference = paymentStatusChargebackDto.getFailureReference();
        Response chargebackResponse = paymentTestService.postChargeback(
            USER_TOKEN_PAYMENT,
            paymentStatusChargebackDto);

        PaymentFailureResponse paymentsFailureResponse =
            paymentTestService.getFailurePayment(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentStatusChargebackDto.getPaymentReference()).then()
                .statusCode(OK.value()).extract().as(PaymentFailureResponse.class);

        assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getFailureReference()).isEqualTo(paymentStatusChargebackDto.getFailureReference());

        assertThat(chargebackResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        Response casePaymentGroupResponse1
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN, ccdCaseNumber);

        PaymentGroupResponse paymentGroupResponse1
            = casePaymentGroupResponse1.getBody().as(PaymentGroupResponse.class);

        assertThat(paymentGroupResponse1.getPaymentGroups().get(0).getServiceRequestStatus())
            .isEqualTo(DISPUTED);

        // Ping 2
        DateTime actualDateTime = new DateTime(System.currentTimeMillis());
        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
            .representmentStatus(RepresentmentStatus.Yes)
            .representmentDate(actualDateTime.plusMinutes(15).toString())
            .build();
        Response ping2Response = paymentTestService.paymentStatusSecond(
            SERVICE_TOKEN, paymentStatusChargebackDto.getFailureReference(),
            paymentStatusUpdateSecond);

        assertEquals(ping2Response.getStatusCode(), OK.value());
        assertEquals("successful operation", ping2Response.getBody().prettyPrint());

        Response casePaymentGroupResponse2
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN,ccdCaseNumber);

        PaymentGroupResponse paymentGroupResponse2
            = casePaymentGroupResponse2.getBody().as(PaymentGroupResponse.class);

        assertThat(paymentGroupResponse2.getPaymentGroups().get(0).getServiceRequestStatus())
            .isEqualTo(PAID);
   }

    @Test
    public void return_partially_paid_when_failure_event_and_HMCTS_lost_dispute_ping_two() {
        String ccdCaseNumber = "11111235" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        FeeDto feeDto = FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("550.00"))
            .ccdCaseNumber(ccdCaseNumber)
            .version("4")
            .code("FEE0002")
            .description("Application for a third party debt order")
            .build();

        TelephonyCardPaymentsRequest telephonyPaymentRequest = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(new BigDecimal("550"))
            .ccdCaseNumber(ccdCaseNumber)
            .currency(CurrencyCode.GBP)
            .caseType("DIVORCE")
             .telephonySystem(TelephonySystem.DEFAULT_SYSTEM_NAME)
            .returnURL("https://www.moneyclaims.service.gov.uk")
            .build();

        PaymentGroupDto groupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Arrays.asList(feeDto)).build();

        dsl.given().userToken(USER_TOKEN_CARD_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(groupDto)
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                assertThat(paymentGroupFeeDto).isNotNull();

                String paymentGroupReference = paymentGroupFeeDto.getPaymentGroupReference();

                dsl.given().userToken(USER_TOKEN_CARD_PAYMENT)
                    .s2sToken(SERVICE_TOKEN)
                    .returnUrl("https://www.moneyclaims.service.gov.uk")
                    .when().createTelephonyPayment(telephonyPaymentRequest, paymentGroupReference)
                    .then().gotCreated(TelephonyCardPaymentsResponse.class, telephonyCardPaymentsResponse -> {
                        assertThat(telephonyCardPaymentsResponse).isNotNull();
                        assertThat(telephonyCardPaymentsResponse.getStatus()).isEqualTo("Initiated");
                        paymentReference = telephonyCardPaymentsResponse.getPaymentReference();
                    });
                // pci-pal callback
                TelephonyCallbackDto callbackDto = TelephonyCallbackDto.telephonyCallbackWith()
                    .orderReference(paymentReference)
                    .orderAmount("550")
                    .transactionResult("SUCCESS")
                    .build();

                dsl.given().s2sToken(SERVICE_TOKEN)
                    .when().telephonyCallback(callbackDto)
                    .then().noContent();

            });
        Response casePaymentGroupResponse
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN, ccdCaseNumber);
        PaymentGroupResponse paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        assertThat(paymentGroupResponse.getPaymentGroups().get(0).getServiceRequestStatus())
            .isEqualTo(PAID);

        PaymentStatusChargebackDto paymentStatusChargebackDto
            = PaymentFixture.chargebackRequestService(paymentReference,ccdCaseNumber);
        paymentFailureReference = paymentStatusChargebackDto.getFailureReference();
        Response chargebackResponse = paymentTestService.postChargeback(
            USER_TOKEN_PAYMENT,
            paymentStatusChargebackDto);

        PaymentFailureResponse paymentsFailureResponse =
            paymentTestService.getFailurePayment(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentStatusChargebackDto.getPaymentReference()).then()
                .statusCode(OK.value()).extract().as(PaymentFailureResponse.class);

        assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getFailureReference()).isEqualTo(paymentStatusChargebackDto.getFailureReference());

        assertThat(chargebackResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        Response casePaymentGroupResponse1
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN, ccdCaseNumber);

        PaymentGroupResponse paymentGroupResponse1
            = casePaymentGroupResponse1.getBody().as(PaymentGroupResponse.class);

        assertThat(paymentGroupResponse1.getPaymentGroups().get(0).getServiceRequestStatus())
            .isEqualTo(DISPUTED);

        // Ping 2
        DateTime actualDateTime = new DateTime(System.currentTimeMillis());
        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
            .representmentStatus(RepresentmentStatus.No)
            .representmentDate(actualDateTime.plusMinutes(15).toString())
            .build();
        Response ping2Response = paymentTestService.paymentStatusSecond(
            SERVICE_TOKEN, paymentStatusChargebackDto.getFailureReference(),
            paymentStatusUpdateSecond);

        assertEquals(ping2Response.getStatusCode(), OK.value());
        assertEquals("successful operation", ping2Response.getBody().prettyPrint());

        Response casePaymentGroupResponse2
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN, ccdCaseNumber);

        PaymentGroupResponse paymentGroupResponse2
            = casePaymentGroupResponse2.getBody().as(PaymentGroupResponse.class);

        assertThat(paymentGroupResponse2.getPaymentGroups().get(0).getServiceRequestStatus())
            .isEqualTo(PARTIALLY_PAID);
    }

    @Test
    public void return_paid_when_failure_event_and_HMCTS_received_money_retro_remission_ping_two() {

        // Create a Bulk scan payment
        String ccdCaseNumber = "1111-CC12" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        String dcn = "6600000000001" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        dcn=  dcn.substring(0,21);
        BulkScanPaymentRequest bulkScanPaymentRequest = BulkScanPaymentRequest.createBulkScanPaymentWith()
            .amount(new BigDecimal("100.00"))
            .service("DIVORCE")
            .siteId("AA01")
            .currency(CurrencyCode.GBP)
            .documentControlNumber(dcn)
            .ccdCaseNumber(ccdCaseNumber)
            .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
            .payerName("CCD User1")
            .bankedDate(DateTime.now().toString())
            .paymentMethod(PaymentMethodType.CHEQUE)
            .paymentStatus(PaymentStatus.SUCCESS)
            .giroSlipNo("GH716376")
            .build();

        PaymentGroupDto paymentGroupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Collections.singletonList(FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("100.00"))
                .code("FEE3132")
                .version("1")
                .reference("testRef1")
                .volume(2)
                .ccdCaseNumber(ccdCaseNumber)
                .build())).build();

        AtomicReference<String> paymentReference = new AtomicReference<>();

        dsl.given().userToken(USER_TOKEN_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(paymentGroupDto)
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                assertThat(paymentGroupFeeDto).isNotNull();
                assertThat(paymentGroupFeeDto.getPaymentGroupReference()).isNotNull();
                assertThat(paymentGroupFeeDto.getFees().get(0)).isEqualToComparingOnlyGivenFields(paymentGroupDto);

                dsl.given().userToken(USER_TOKEN_PAYMENT)
                    .s2sToken(SERVICE_TOKEN)
                    .when().createBulkScanPayment(bulkScanPaymentRequest, paymentGroupFeeDto.getPaymentGroupReference())
                    .then().gotCreated(PaymentDto.class, paymentDto -> {
                        assertThat(paymentDto.getReference()).isNotNull();
                        assertThat(paymentDto.getStatus()).isEqualToIgnoringCase("success");
                        assertThat(paymentDto.getPaymentGroupReference()).isEqualTo(paymentGroupFeeDto.getPaymentGroupReference());

                        paymentReference.set(paymentDto.getReference());

                    });

            });
        Response casePaymentGroupResponse
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN, ccdCaseNumber);
        PaymentGroupResponse paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        assertThat(paymentGroupResponse.getPaymentGroups().get(0).getServiceRequestStatus())
            .isEqualTo(PAID);
        final String paymentGroupReference = paymentGroupResponse.getPaymentGroups().get(0).getPaymentGroupReference();
        final Integer feeId = paymentGroupResponse.getPaymentGroups().get(0).getFees().get(0).getId();

        //TEST create retrospective remission
        Response response = dsl.given().userToken(USER_TOKEN_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("100.00"), paymentGroupReference, feeId)
            .then().getResponse();
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        // Ping 1 for Bounced Cheque event
        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto
            = PaymentFixture.bouncedChequeRequestService(paymentReference.get(),ccdCaseNumber);

        Response bounceChequeResponse = paymentTestService.postBounceCheque(
            SERVICE_TOKEN,
            paymentStatusBouncedChequeDto);

        assertThat(bounceChequeResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        Response casePaymentGroupResponse1
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN, ccdCaseNumber);

        PaymentGroupResponse paymentGroupResponse1
            = casePaymentGroupResponse1.getBody().as(PaymentGroupResponse.class);

        assertThat(paymentGroupResponse1.getPaymentGroups().get(0).getServiceRequestStatus())
            .isEqualTo(DISPUTED);

        // Ping 2
        DateTime actualDateTime = new DateTime(System.currentTimeMillis());
        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
            .representmentStatus(RepresentmentStatus.Yes)
            .representmentDate(actualDateTime.plusMinutes(15).toString())
            .build();
        Response ping2Response = paymentTestService.paymentStatusSecond(
            SERVICE_TOKEN, paymentStatusBouncedChequeDto.getFailureReference(),
            paymentStatusUpdateSecond);

        assertEquals(ping2Response.getStatusCode(), OK.value());
        assertEquals("successful operation", ping2Response.getBody().prettyPrint());

        Response casePaymentGroupResponse2
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN, ccdCaseNumber);

        PaymentGroupResponse paymentGroupResponse2
            = casePaymentGroupResponse2.getBody().as(PaymentGroupResponse.class);

        assertThat(paymentGroupResponse2.getPaymentGroups().get(0).getServiceRequestStatus())
            .isEqualTo(PAID);

        // delete payment record
        paymentTestService.deletePayment(USER_TOKEN_CMC_SOLICITOR, SERVICE_TOKEN, paymentReference.get()).then().statusCode(NO_CONTENT.value());

        //delete Payment Failure record
        paymentTestService.deleteFailedPayment(USER_TOKEN_CMC_SOLICITOR, SERVICE_TOKEN, paymentStatusBouncedChequeDto.getFailureReference()).then().statusCode(NO_CONTENT.value());
    }

    @Test
    public void return_disputed_when_failure_event_has_happen_full_remission_ping_one() {

        // Create a Bulk scan payment
        String ccdCaseNumber = "1111-CC12" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        String dcn = "6600000000001" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        dcn=  dcn.substring(0,21);
        BulkScanPaymentRequest bulkScanPaymentRequest = BulkScanPaymentRequest.createBulkScanPaymentWith()
            .amount(new BigDecimal("100.00"))
            .service("DIVORCE")
            .siteId("AA01")
            .currency(CurrencyCode.GBP)
            .documentControlNumber(dcn)
            .ccdCaseNumber(ccdCaseNumber)
            .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
            .payerName("CCD User1")
            .bankedDate(DateTime.now().toString())
            .paymentMethod(PaymentMethodType.CHEQUE)
            .paymentStatus(PaymentStatus.SUCCESS)
            .giroSlipNo("GH716376")
            .build();

        PaymentGroupDto paymentGroupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Collections.singletonList(FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("100.00"))
                .code("FEE3132")
                .version("1")
                .reference("testRef1")
                .volume(2)
                .ccdCaseNumber(ccdCaseNumber)
                .build())).build();

        dsl.given().userToken(USER_TOKEN_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(paymentGroupDto)
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                assertThat(paymentGroupFeeDto).isNotNull();
                assertThat(paymentGroupFeeDto.getPaymentGroupReference()).isNotNull();
                assertThat(paymentGroupFeeDto.getFees().get(0)).isEqualToComparingOnlyGivenFields(paymentGroupDto);

                dsl.given().userToken(USER_TOKEN_PAYMENT)
                    .s2sToken(SERVICE_TOKEN)
                    .when().createBulkScanPayment(bulkScanPaymentRequest, paymentGroupFeeDto.getPaymentGroupReference())
                    .then().gotCreated(PaymentDto.class, paymentDto -> {
                        assertThat(paymentDto.getReference()).isNotNull();
                        assertThat(paymentDto.getStatus()).isEqualToIgnoringCase("success");
                        assertThat(paymentDto.getPaymentGroupReference()).isEqualTo(paymentGroupFeeDto.getPaymentGroupReference());

                        paymentReference = paymentDto.getReference();

                    });

            });
        Response casePaymentGroupResponse
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN, ccdCaseNumber);
        PaymentGroupResponse paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        assertThat(paymentGroupResponse.getPaymentGroups().get(0).getServiceRequestStatus())
            .isEqualTo(PAID);
        final String paymentGroupReference = paymentGroupResponse.getPaymentGroups().get(0).getPaymentGroupReference();
        final Integer feeId = paymentGroupResponse.getPaymentGroups().get(0).getFees().get(0).getId();

        //TEST create retrospective remission
        Response response = dsl.given().userToken(USER_TOKEN_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("100.00"), paymentGroupReference, feeId)
            .then().getResponse();
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        // Ping 1 for Bounced Cheque event
        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto
            = PaymentFixture.bouncedChequeRequestService(paymentReference,ccdCaseNumber);
        paymentFailureReference = paymentStatusBouncedChequeDto.getFailureReference();
        Response bounceChequeResponse = paymentTestService.postBounceCheque(
            SERVICE_TOKEN,
            paymentStatusBouncedChequeDto);

        assertThat(bounceChequeResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        Response casePaymentGroupResponse1
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN, ccdCaseNumber);

        PaymentGroupResponse paymentGroupResponse1
            = casePaymentGroupResponse1.getBody().as(PaymentGroupResponse.class);

        assertThat(paymentGroupResponse1.getPaymentGroups().get(0).getServiceRequestStatus())
            .isEqualTo(DISPUTED);
    }

    @Test
    public void return_paid_when_failure_event_and_HMCTS_not_received_money_full_retro_remission_ping_two() {

        // Create a Bulk scan payment
        String ccdCaseNumber = "1111-CC12" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        String dcn = "6600000000001" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        dcn=  dcn.substring(0,21);
        BulkScanPaymentRequest bulkScanPaymentRequest = BulkScanPaymentRequest.createBulkScanPaymentWith()
            .amount(new BigDecimal("100.00"))
            .service("DIVORCE")
            .siteId("AA01")
            .currency(CurrencyCode.GBP)
            .documentControlNumber(dcn)
            .ccdCaseNumber(ccdCaseNumber)
            .paymentChannel(PaymentChannel.paymentChannelWith().name("bulk scan").build())
            .payerName("CCD User1")
            .bankedDate(DateTime.now().toString())
            .paymentMethod(PaymentMethodType.CHEQUE)
            .paymentStatus(PaymentStatus.SUCCESS)
            .giroSlipNo("GH716376")
            .build();

        PaymentGroupDto paymentGroupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Collections.singletonList(FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("100.00"))
                .code("FEE3132")
                .version("1")
                .reference("testRef1")
                .volume(2)
                .ccdCaseNumber(ccdCaseNumber)
                .build())).build();

        dsl.given().userToken(USER_TOKEN_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(paymentGroupDto)
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                assertThat(paymentGroupFeeDto).isNotNull();
                assertThat(paymentGroupFeeDto.getPaymentGroupReference()).isNotNull();
                assertThat(paymentGroupFeeDto.getFees().get(0)).isEqualToComparingOnlyGivenFields(paymentGroupDto);

                dsl.given().userToken(USER_TOKEN_PAYMENT)
                    .s2sToken(SERVICE_TOKEN)
                    .when().createBulkScanPayment(bulkScanPaymentRequest, paymentGroupFeeDto.getPaymentGroupReference())
                    .then().gotCreated(PaymentDto.class, paymentDto -> {
                        assertThat(paymentDto.getReference()).isNotNull();
                        assertThat(paymentDto.getStatus()).isEqualToIgnoringCase("success");
                        assertThat(paymentDto.getPaymentGroupReference()).isEqualTo(paymentGroupFeeDto.getPaymentGroupReference());

                        paymentReference = paymentDto.getReference();
                    });

            });
        Response casePaymentGroupResponse
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN, ccdCaseNumber);
        PaymentGroupResponse paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);
        assertThat(paymentGroupResponse.getPaymentGroups().get(0).getServiceRequestStatus())
            .isEqualTo(PAID);
        final String paymentGroupReference = paymentGroupResponse.getPaymentGroups().get(0).getPaymentGroupReference();
        final Integer feeId = paymentGroupResponse.getPaymentGroups().get(0).getFees().get(0).getId();


        // Ping 1 for Bounced Cheque event
        PaymentStatusBouncedChequeDto paymentStatusBouncedChequeDto
            = PaymentFixture.bouncedChequeRequestService(paymentReference,ccdCaseNumber);
        paymentFailureReference = paymentStatusBouncedChequeDto.getFailureReference();
        Response bounceChequeResponse = paymentTestService.postBounceCheque(
            SERVICE_TOKEN,
            paymentStatusBouncedChequeDto);

        assertThat(bounceChequeResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        Response casePaymentGroupResponse1
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN, ccdCaseNumber);

        PaymentGroupResponse paymentGroupResponse1
            = casePaymentGroupResponse1.getBody().as(PaymentGroupResponse.class);

        assertThat(paymentGroupResponse1.getPaymentGroups().get(0).getServiceRequestStatus())
            .isEqualTo(DISPUTED);

        // Ping 2
        DateTime actualDateTime = new DateTime(System.currentTimeMillis());
        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
            .representmentStatus(RepresentmentStatus.No)
            .representmentDate(actualDateTime.plusMinutes(15).toString())
            .build();
        Response ping2Response = paymentTestService.paymentStatusSecond(
            SERVICE_TOKEN, paymentStatusBouncedChequeDto.getFailureReference(),
            paymentStatusUpdateSecond);

        assertEquals(ping2Response.getStatusCode(), OK.value());
        assertEquals("successful operation", ping2Response.getBody().prettyPrint());

        //TEST create retrospective remission
        Response response = dsl.given().userToken(USER_TOKEN_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("100.00"), paymentGroupReference, feeId)
            .then().getResponse();
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());


        Response casePaymentGroupResponse2
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN, ccdCaseNumber);

        PaymentGroupResponse paymentGroupResponse2
            = casePaymentGroupResponse2.getBody().as(PaymentGroupResponse.class);

        assertThat(paymentGroupResponse2.getPaymentGroups().get(0).getServiceRequestStatus())
            .isEqualTo(PAID);
    }

    @Test
    public void return_disputed_when_failure_event_has_happen_partial_remission_ping_one() {

        // Create a Telephony payment
        String ccdCaseNumber = "11111234" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        FeeDto feeDto = FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("550.00"))
            .ccdCaseNumber(ccdCaseNumber)
            .version("4")
            .code("FEE0002")
            .description("Application for a third party debt order")
            .build();

        TelephonyCardPaymentsRequest telephonyPaymentRequest = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(new BigDecimal("500"))
            .ccdCaseNumber(ccdCaseNumber)
            .currency(CurrencyCode.GBP)
            .caseType("DIVORCE")
             .telephonySystem(TelephonySystem.DEFAULT_SYSTEM_NAME)
            .returnURL("https://www.moneyclaims.service.gov.uk")
            .build();

        PaymentGroupDto groupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Arrays.asList(feeDto)).build();

        dsl.given().userToken(USER_TOKEN_CARD_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(groupDto)
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                assertThat(paymentGroupFeeDto).isNotNull();

                String paymentGroupReference = paymentGroupFeeDto.getPaymentGroupReference();

                dsl.given().userToken(USER_TOKEN_CARD_PAYMENT)
                    .s2sToken(SERVICE_TOKEN)
                    .returnUrl("https://www.moneyclaims.service.gov.uk")
                    .when().createTelephonyPayment(telephonyPaymentRequest, paymentGroupReference)
                    .then().gotCreated(TelephonyCardPaymentsResponse.class, telephonyCardPaymentsResponse -> {
                        assertThat(telephonyCardPaymentsResponse).isNotNull();
                        assertThat(telephonyCardPaymentsResponse.getStatus()).isEqualTo("Initiated");
                        paymentReference = telephonyCardPaymentsResponse.getPaymentReference();
                    });
                // pci-pal callback
                TelephonyCallbackDto callbackDto = TelephonyCallbackDto.telephonyCallbackWith()
                    .orderReference(paymentReference)
                    .orderAmount("550")
                    .transactionResult("SUCCESS")
                    .build();

                dsl.given().s2sToken(SERVICE_TOKEN)
                    .when().telephonyCallback(callbackDto)
                    .then().noContent();

            });


        Response casePaymentGroupResponse
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN, ccdCaseNumber);
        PaymentGroupResponse paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);

        final String paymentGroupReference = paymentGroupResponse.getPaymentGroups().get(0).getPaymentGroupReference();
        final Integer feeId = paymentGroupResponse.getPaymentGroups().get(0).getFees().get(0).getId();


        //TEST create retrospective remission
        Response response = dsl.given().userToken(USER_TOKEN_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("50.00"), paymentGroupReference, feeId)
            .then().getResponse();
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        Response casePaymentGroupResponse1
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN, ccdCaseNumber);
        PaymentGroupResponse paymentGroupResponse1
            = casePaymentGroupResponse1.getBody().as(PaymentGroupResponse.class);
        assertThat(paymentGroupResponse1.getPaymentGroups().get(0).getServiceRequestStatus())
            .isEqualTo(PAID);

        // Ping 1 for Chargeback event
        PaymentStatusChargebackDto paymentStatusChargebackDto
            = PaymentFixture.chargebackRequestService(paymentReference,ccdCaseNumber);
        paymentFailureReference = paymentStatusChargebackDto.getFailureReference();
        Response chargebackResponse = paymentTestService.postChargeback(
            USER_TOKEN_CMC_SOLICITOR,
            paymentStatusChargebackDto);

        PaymentFailureResponse paymentsFailureResponse =
            paymentTestService.getFailurePayment(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentStatusChargebackDto.getPaymentReference()).then()
                .statusCode(OK.value()).extract().as(PaymentFailureResponse.class);

        assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getFailureReference()).isEqualTo(paymentStatusChargebackDto.getFailureReference());

        assertThat(chargebackResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());

        Response casePaymentGroupResponse2
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN, ccdCaseNumber);

        PaymentGroupResponse paymentGroupResponse2
            = casePaymentGroupResponse2.getBody().as(PaymentGroupResponse.class);

        assertThat(paymentGroupResponse2.getPaymentGroups().get(0).getServiceRequestStatus())
            .isEqualTo(DISPUTED);
  }

    @Test
    public void return_paid_when_failure_event_has_happen_partial_remission__HMCTS_received_money_ping_two() {

        // Create a Telephony payment
        String ccdCaseNumber = "11111234" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        FeeDto feeDto = FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("550.00"))
            .ccdCaseNumber(ccdCaseNumber)
            .version("4")
            .code("FEE0002")
            .description("Application for a third party debt order")
            .build();

        TelephonyCardPaymentsRequest telephonyPaymentRequest = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(new BigDecimal("500"))
            .ccdCaseNumber(ccdCaseNumber)
            .currency(CurrencyCode.GBP)
            .caseType("DIVORCE")
             .telephonySystem(TelephonySystem.DEFAULT_SYSTEM_NAME)
            .returnURL("https://www.moneyclaims.service.gov.uk")
            .build();

        PaymentGroupDto groupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Arrays.asList(feeDto)).build();

        dsl.given().userToken(USER_TOKEN_CARD_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(groupDto)
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                assertThat(paymentGroupFeeDto).isNotNull();

                String paymentGroupReference = paymentGroupFeeDto.getPaymentGroupReference();

                dsl.given().userToken(USER_TOKEN_CARD_PAYMENT)
                    .s2sToken(SERVICE_TOKEN)
                    .returnUrl("https://www.moneyclaims.service.gov.uk")
                    .when().createTelephonyPayment(telephonyPaymentRequest, paymentGroupReference)
                    .then().gotCreated(TelephonyCardPaymentsResponse.class, telephonyCardPaymentsResponse -> {
                        assertThat(telephonyCardPaymentsResponse).isNotNull();
                        assertThat(telephonyCardPaymentsResponse.getStatus()).isEqualTo("Initiated");
                        paymentReference = telephonyCardPaymentsResponse.getPaymentReference();
                    });
                // pci-pal callback
                TelephonyCallbackDto callbackDto = TelephonyCallbackDto.telephonyCallbackWith()
                    .orderReference(paymentReference)
                    .orderAmount("550")
                    .transactionResult("SUCCESS")
                    .build();

                dsl.given().s2sToken(SERVICE_TOKEN)
                    .when().telephonyCallback(callbackDto)
                    .then().noContent();

            });


        Response casePaymentGroupResponse
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN, ccdCaseNumber);
        PaymentGroupResponse paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);

        final String paymentGroupReference = paymentGroupResponse.getPaymentGroups().get(0).getPaymentGroupReference();
        final Integer feeId = paymentGroupResponse.getPaymentGroups().get(0).getFees().get(0).getId();


        //TEST create retrospective remission
        Response response = dsl.given().userToken(USER_TOKEN_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("50.00"), paymentGroupReference, feeId)
            .then().getResponse();
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        Response casePaymentGroupResponse1
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN, ccdCaseNumber);
        PaymentGroupResponse paymentGroupResponse1
            = casePaymentGroupResponse1.getBody().as(PaymentGroupResponse.class);
        assertThat(paymentGroupResponse1.getPaymentGroups().get(0).getServiceRequestStatus())
            .isEqualTo(PAID);

        // Ping 1 for Chargeback event
        PaymentStatusChargebackDto paymentStatusChargebackDto
            = PaymentFixture.chargebackRequestService(paymentReference,ccdCaseNumber);
        paymentFailureReference = paymentStatusChargebackDto.getFailureReference();
        Response chargebackResponse = paymentTestService.postChargeback(
            USER_TOKEN_CMC_SOLICITOR,
            paymentStatusChargebackDto);

        PaymentFailureResponse paymentsFailureResponse =
            paymentTestService.getFailurePayment(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentStatusChargebackDto.getPaymentReference()).then()
                .statusCode(OK.value()).extract().as(PaymentFailureResponse.class);

        assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getFailureReference()).isEqualTo(paymentStatusChargebackDto.getFailureReference());

        assertThat(chargebackResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());

        Response casePaymentGroupResponse2
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN, ccdCaseNumber);

        PaymentGroupResponse paymentGroupResponse2
            = casePaymentGroupResponse2.getBody().as(PaymentGroupResponse.class);

        assertThat(paymentGroupResponse2.getPaymentGroups().get(0).getServiceRequestStatus())
            .isEqualTo(DISPUTED);

        // Ping 2
        DateTime actualDateTime = new DateTime(System.currentTimeMillis());
        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
            .representmentStatus(RepresentmentStatus.Yes)
            .representmentDate(actualDateTime.plusMinutes(15).toString())
            .build();
        Response ping2Response = paymentTestService.paymentStatusSecond(
            SERVICE_TOKEN, paymentStatusChargebackDto.getFailureReference(),
            paymentStatusUpdateSecond);

        assertEquals(ping2Response.getStatusCode(), OK.value());
        assertEquals("successful operation", ping2Response.getBody().prettyPrint());

        Response casePaymentGroupResponse3
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN, ccdCaseNumber);

        PaymentGroupResponse paymentGroupResponse3
            = casePaymentGroupResponse3.getBody().as(PaymentGroupResponse.class);

        assertThat(paymentGroupResponse3.getPaymentGroups().get(0).getServiceRequestStatus())
            .isEqualTo(PAID);
    }

    @Test
    public void return_partially_paid_when_failure_event_has_happen_partial_remission__HMCTS_received_money_ping_two() {

        // Create a Telephony payment
        String ccdCaseNumber = "11111236" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        FeeDto feeDto = FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("550.00"))
            .ccdCaseNumber(ccdCaseNumber)
            .version("4")
            .code("FEE0002")
            .description("Application for a third party debt order")
            .build();

        TelephonyCardPaymentsRequest telephonyPaymentRequest = TelephonyCardPaymentsRequest.telephonyCardPaymentsRequestWith()
            .amount(new BigDecimal("500"))
            .ccdCaseNumber(ccdCaseNumber)
            .currency(CurrencyCode.GBP)
            .caseType("DIVORCE")
             .telephonySystem(TelephonySystem.DEFAULT_SYSTEM_NAME)
            .returnURL("https://www.moneyclaims.service.gov.uk")
            .build();

        PaymentGroupDto groupDto = PaymentGroupDto.paymentGroupDtoWith()
            .fees(Arrays.asList(feeDto)).build();

        dsl.given().userToken(USER_TOKEN_CARD_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .when().addNewFeeAndPaymentGroup(groupDto)
            .then().gotCreated(PaymentGroupDto.class, paymentGroupFeeDto -> {
                assertThat(paymentGroupFeeDto).isNotNull();

                String paymentGroupReference = paymentGroupFeeDto.getPaymentGroupReference();

                dsl.given().userToken(USER_TOKEN_CARD_PAYMENT)
                    .s2sToken(SERVICE_TOKEN)
                    .returnUrl("https://www.moneyclaims.service.gov.uk")
                    .when().createTelephonyPayment(telephonyPaymentRequest, paymentGroupReference)
                    .then().gotCreated(TelephonyCardPaymentsResponse.class, telephonyCardPaymentsResponse -> {
                        assertThat(telephonyCardPaymentsResponse).isNotNull();
                        assertThat(telephonyCardPaymentsResponse.getStatus()).isEqualTo("Initiated");
                        paymentReference = telephonyCardPaymentsResponse.getPaymentReference();
                    });
                // pci-pal callback
                TelephonyCallbackDto callbackDto = TelephonyCallbackDto.telephonyCallbackWith()
                    .orderReference(paymentReference)
                    .orderAmount("550")
                    .transactionResult("SUCCESS")
                    .build();

                dsl.given().s2sToken(SERVICE_TOKEN)
                    .when().telephonyCallback(callbackDto)
                    .then().noContent();

            });


        Response casePaymentGroupResponse
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN, ccdCaseNumber);
        PaymentGroupResponse paymentGroupResponse
            = casePaymentGroupResponse.getBody().as(PaymentGroupResponse.class);

        final String paymentGroupReference = paymentGroupResponse.getPaymentGroups().get(0).getPaymentGroupReference();
        final Integer feeId = paymentGroupResponse.getPaymentGroups().get(0).getFees().get(0).getId();


        //TEST create retrospective remission
        Response response = dsl.given().userToken(USER_TOKEN_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .when().createRetrospectiveRemissionForRefund(getRetroRemissionRequest("50.00"), paymentGroupReference, feeId)
            .then().getResponse();
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        Response casePaymentGroupResponse1
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN, ccdCaseNumber);
        PaymentGroupResponse paymentGroupResponse1
            = casePaymentGroupResponse1.getBody().as(PaymentGroupResponse.class);
        assertThat(paymentGroupResponse1.getPaymentGroups().get(0).getServiceRequestStatus())
            .isEqualTo(PAID);

        // Ping 1 for Chargeback event
        PaymentStatusChargebackDto paymentStatusChargebackDto
            = PaymentFixture.chargebackRequestService(paymentReference,ccdCaseNumber);
        paymentFailureReference = paymentStatusChargebackDto.getFailureReference();
        Response chargebackResponse = paymentTestService.postChargeback(
            USER_TOKEN_CMC_SOLICITOR,
            paymentStatusChargebackDto);

        PaymentFailureResponse paymentsFailureResponse =
            paymentTestService.getFailurePayment(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentStatusChargebackDto.getPaymentReference()).then()
                .statusCode(OK.value()).extract().as(PaymentFailureResponse.class);

        assertThat(paymentsFailureResponse.getPaymentFailureList().get(0).getPaymentFailureInitiated().getFailureReference()).isEqualTo(paymentStatusChargebackDto.getFailureReference());

        assertThat(chargebackResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());

        Response casePaymentGroupResponse2
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN, ccdCaseNumber);

        PaymentGroupResponse paymentGroupResponse2
            = casePaymentGroupResponse2.getBody().as(PaymentGroupResponse.class);

        assertThat(paymentGroupResponse2.getPaymentGroups().get(0).getServiceRequestStatus())
            .isEqualTo(DISPUTED);

        // Ping 2
        DateTime actualDateTime = new DateTime(System.currentTimeMillis());
        PaymentStatusUpdateSecond paymentStatusUpdateSecond = PaymentStatusUpdateSecond.paymentStatusUpdateSecondWith()
            .representmentStatus(RepresentmentStatus.No)
            .representmentDate(actualDateTime.plusMinutes(15).toString())
            .build();
        Response ping2Response = paymentTestService.paymentStatusSecond(
            SERVICE_TOKEN, paymentStatusChargebackDto.getFailureReference(),
            paymentStatusUpdateSecond);

        assertEquals(ping2Response.getStatusCode(), OK.value());
        assertEquals("successful operation", ping2Response.getBody().prettyPrint());

        Response casePaymentGroupResponse3
            = cardTestService
            .getPaymentGroupsForCase(USER_TOKEN_PAYMENT, SERVICE_TOKEN, ccdCaseNumber);

        PaymentGroupResponse paymentGroupResponse3
            = casePaymentGroupResponse3.getBody().as(PaymentGroupResponse.class);

        assertThat(paymentGroupResponse3.getPaymentGroups().get(0).getServiceRequestStatus())
            .isEqualTo(PARTIALLY_PAID);
    }

    @Test
    public void createUpfrontRemission() throws Exception {

        dsl.given().userToken(USER_TOKEN_PAYMENT)
            .s2sToken(SERVICE_TOKEN)
            .when().createUpfrontRemission(getRemissionRequest())
            .then().gotCreated(RemissionDto.class, remissionDto -> {
                assertThat(remissionDto).isNotNull();
                assertThat(remissionDto.getFee()).isEqualToComparingOnlyGivenFields(getFee());
            });

    }

    private RemissionRequest getRemissionRequest() {
        return RemissionRequest.createRemissionRequestWith()
            .beneficiaryName("A partial remission")
            .ccdCaseNumber("1111-2222-3333-4444")
            .hwfAmount(new BigDecimal("550.00"))
            .hwfReference("HR1111")
            .caseType("DIVORCE_ExceptionRecord")
            .fee(getFee())
            .build();
    }

    private FeeDto getFee() {
        return FeeDto.feeDtoWith()
            .calculatedAmount(new BigDecimal("550.00"))
            .ccdCaseNumber("1111-2222-3333-4444")
            .version("1")
            .code("FEE0123")
            .build();
    }


    private static RetroRemissionRequest getRetroRemissionRequest(final String remissionAmount) {
        return RetroRemissionRequest.createRetroRemissionRequestWith()
            .hwfAmount(new BigDecimal(remissionAmount))
            .hwfReference("HWF-A1B-23C")
            .build();
    }

    private void verifyThePaymentGroupResponseForNoRemisssions(final ServiceRequestDto serviceRequestDto,
                                                               final PaymentGroupResponse paymentGroupResponse)
        throws Exception {
        paymentGroupResponse.getPaymentGroups().stream().forEach(paymentGroupDto -> {
            assertThat(paymentGroupDto.getRemissions()).isNullOrEmpty();
            assertThat(paymentGroupDto.getPaymentGroupReference()).matches(SERVICE_REQUEST_REGEX_PATTERN);
            assertThat(paymentGroupDto.getFees().get(0).getCode().equals(serviceRequestDto.getFees().get(0).getCode())).isTrue();
            assertThat(paymentGroupDto.getServiceRequestStatus().equals(NOT_PAID)).isTrue();
            assertThat(paymentGroupDto.getDateCreated()).isNotNull();
            try {
                verifyDate(paymentGroupDto.getDateCreated());
                verifyDate(paymentGroupDto.getDateUpdated());
            } catch (ParseException e) {
                e.printStackTrace();
                fail("The Date Check has failed....");
            }
        });
    }

    private static void verifyThePaymentGroupResponseForUsers(final ServiceRequestDto serviceRequestDto,
                                                              final PaymentGroupResponse paymentGroupResponse,
                                                              final boolean isPaymentsRole,
                                                              final boolean paymentStatusFlag)
        throws Exception {
        paymentGroupResponse.getPaymentGroups().stream().forEach(paymentGroupDto -> {
            if (isPaymentsRole) {
                assertThat(paymentGroupDto.getPayments()).isNotNull();
                assertThat(paymentGroupDto.getPayments().get(0).getAmount())
                    .isEqualTo(serviceRequestDto.getFees().get(0).getCalculatedAmount());
                assertThat(paymentGroupDto.getPayments().get(0).getCcdCaseNumber()).isEqualTo(serviceRequestDto.getCcdCaseNumber());
                assertThat(paymentGroupDto.getRemissions().size()).isEqualTo(0);
            }
            assertThat(paymentGroupDto.getPaymentGroupReference()).matches(SERVICE_REQUEST_REGEX_PATTERN);
            assertThat(paymentGroupDto.getFees().get(0).getCode().equals(serviceRequestDto.getFees().get(0).getCode())).isTrue();
            if (paymentStatusFlag) {
                assertThat(paymentGroupDto.getServiceRequestStatus().equals(PAID)).isTrue();
            } else {
                //This is not the expected status but because the Status has to be updated from Gov pay and we cannot do that in an easy manner.
                //The Status is left as NOT_PAID as the Next Url given by Gov pay is a manually user driven screen and there is not API update from Gov Pay to Payment App.
                //To be Tested from the Front End...
                assertThat(paymentGroupDto.getServiceRequestStatus().equals(NOT_PAID)).isTrue();
            }
        });
    }

    private static void verifyDate(Date date) throws ParseException {
        long millisInDay = 60 * 60 * 24 * 1000;
        long createdTime = date.getTime();
        long createdDateOnly = (createdTime / millisInDay) * millisInDay;
        Date createdDateCleared = new Date(createdDateOnly);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date dateWithoutTime = sdf.parse(sdf.format(new Date()));

        assertThat(createdDateCleared).isEqualTo(dateWithoutTime);
    }

    private void verifyMessageOnTheTopic(final ServiceRequestDto serviceRequestDto) throws Exception {

        final String serviceConnectionString = testProps.getServiceRequestCpoUpdateServices2sSecret();
        final String topicName = testProps.getServiceRequestCpoUpdateServices2sTopicName();
        ConnectionStringBuilder connectionStringBuilder = new ConnectionStringBuilder(serviceConnectionString, topicName);
        TopicClient client = new TopicClient(connectionStringBuilder);
        IMessage message = client.peek();
    }

    @After
    public void deletePayment() {
        if (paymentFailureReference != null) {
            //delete Payment Failure record
            paymentTestService.deleteFailedPayment(USER_TOKEN_PAYMENT, SERVICE_TOKEN, paymentFailureReference).then().statusCode(NO_CONTENT.value());
        }
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
