package uk.gov.hmcts.payment.functional;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.TopicClient;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.regex.Pattern;
import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

    @Before
    public void setUp() throws Exception {
        if (!TOKENS_INITIALIZED) {

            USER_TOKEN_PAYMENT = idamService.createUserWith(CMC_CITIZEN_GROUP, "payments").getAuthorisationToken();
            USER_TOKEN_CMC_CITIZEN = idamService.createUserWith(CMC_CITIZEN_GROUP, "citizen").getAuthorisationToken();

            USER_TOKEN_PUI_USER_MANAGER = idamService.createUserWithRefDataEmailFormat(CMC_CASE_WORKER_GROUP,
                "pui-user-manager").getAuthorisationToken();
            USER_TOKEN_PUI_ORGANISATION_MANAGER = idamService.createUserWithRefDataEmailFormat(CMC_CASE_WORKER_GROUP,
                "pui-organisation-manager").getAuthorisationToken();
            USER_TOKEN_PUI_FINANCE_MANAGER = idamService.createUserWithRefDataEmailFormat(CMC_CASE_WORKER_GROUP,
                "pui-finance-manager").getAuthorisationToken();
            USER_TOKEN_PUI_CASE_MANAGER = idamService.createUserWithRefDataEmailFormat(CMC_CASE_WORKER_GROUP,
                "pui-case-manager").getAuthorisationToken();
            USER_TOKEN_CMC_SOLICITOR =
                idamService.createUserWith(CMC_CASE_WORKER_GROUP, "caseworker-cmc-solicitor").getAuthorisationToken();

            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);
            TOKENS_INITIALIZED = true;
        }
    }

    @Test
    public void positive_create_service_request_for_payments_user_hmcts() throws Exception {

        ServiceRequestDto serviceRequestDto
            = ServiceRequestFixture.buildServiceRequestDTO("ABA6", null);
        System.out.println("The Value of the CCD Case Number : "+serviceRequestDto.getCcdCaseNumber());
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
        verifyThePaymentGroupResponseForNoPaymentsOrRemisssions(serviceRequestDto, paymentGroupResponse);
    }

    @Test
//    @Ignore("TODO- Error message is not provided. UPDATE: FIXED")
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
        verifyThePaymentGroupResponseForNoPaymentsOrRemisssions(serviceRequestDto, paymentGroupResponse);

        Response getPaymentGroupResponseForPUIFinanceManager =
            serviceRequestTestService.getPaymentGroups(USER_TOKEN_PUI_FINANCE_MANAGER, SERVICE_TOKEN, serviceRequestDto.getCcdCaseNumber());
        assertThat(getPaymentGroupResponseForPUIFinanceManager.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        PaymentGroupResponse getPaymentGroupResponseForPUIFinanceManagerResponse = getPaymentGroupResponseForPUIFinanceManager.getBody().as(PaymentGroupResponse.class);
        verifyThePaymentGroupResponseForNoPaymentsOrRemisssions(serviceRequestDto, getPaymentGroupResponseForPUIFinanceManagerResponse);

        Response getPaymentGroupResponseForPUICaseManager =
            serviceRequestTestService.getPaymentGroups(USER_TOKEN_PUI_CASE_MANAGER, SERVICE_TOKEN, serviceRequestDto.getCcdCaseNumber());
        assertThat(getPaymentGroupResponseForPUICaseManager.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        PaymentGroupResponse getPaymentGroupResponseForPUICaseManagerResponse = getPaymentGroupResponseForPUICaseManager.getBody().as(PaymentGroupResponse.class);
        verifyThePaymentGroupResponseForNoPaymentsOrRemisssions(serviceRequestDto, getPaymentGroupResponseForPUICaseManagerResponse);

        Response getPaymentGroupResponseForPUIOrganisationManager =
            serviceRequestTestService.getPaymentGroups(USER_TOKEN_PUI_ORGANISATION_MANAGER, SERVICE_TOKEN, serviceRequestDto.getCcdCaseNumber());
        assertThat(getPaymentGroupResponseForPUIOrganisationManager.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        PaymentGroupResponse getPaymentGroupResponseForPUIOrganisationManagerResponse = getPaymentGroupResponseForPUIOrganisationManager.getBody().as(PaymentGroupResponse.class);
        verifyThePaymentGroupResponseForNoPaymentsOrRemisssions(serviceRequestDto, getPaymentGroupResponseForPUIOrganisationManagerResponse);

        Response getPaymentGroupResponseForPUIUserManager =
            serviceRequestTestService.getPaymentGroups(USER_TOKEN_PUI_USER_MANAGER, SERVICE_TOKEN, serviceRequestDto.getCcdCaseNumber());
        assertThat(getPaymentGroupResponseForPUIUserManager.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        PaymentGroupResponse getPaymentGroupResponseForPUIUserManagerResponse = getPaymentGroupResponseForPUIUserManager.getBody().as(PaymentGroupResponse.class);
        verifyThePaymentGroupResponseForNoPaymentsOrRemisssions(serviceRequestDto, getPaymentGroupResponseForPUIUserManagerResponse);

        Response getPaymentGroupResponseForCmcSolicitor =
            serviceRequestTestService.getPaymentGroups(USER_TOKEN_CMC_SOLICITOR, SERVICE_TOKEN, serviceRequestDto.getCcdCaseNumber());
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
    //@Ignore ("No proper Error message for a ccdCaseNumber that is not present")
    public void negative_create_service_request_service_not_found() throws Exception {

        Response getPaymentGroupResponse =
            serviceRequestTestService.getPaymentGroups(USER_TOKEN_PAYMENT, SERVICE_TOKEN, "Unknown");
        assertThat(getPaymentGroupResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(getPaymentGroupResponse.getBody().asString())
            .isEqualTo("No Service found for given CaseType or HMCTS Org Id");
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
        verifyThePaymentGroupResponseForNoPaymentsOrRemisssions(serviceRequestDto, paymentGroupResponse);
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
    //@Ignore("Test Build")
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
    //@Ignore ("A card payment is failing even after a PBA Payment has been made.....")
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
            .currency("GBP")
            .customerReference("123245677").
                build();
        final Response pbaPaymentServiceRequestResponse
            = serviceRequestTestService.createPBAPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
            SERVICE_TOKEN, ServiceRequestFixture.generateUniqueCCDCaseReferenceNumber(),
            serviceRequestReference, serviceRequestPaymentDto);
        assertThat(pbaPaymentServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());

        Response getPaymentGroupResponseForPaymentUser =
            serviceRequestTestService.getPaymentGroups(USER_TOKEN_PAYMENT, SERVICE_TOKEN, serviceRequestDto.getCcdCaseNumber());
        assertThat(getPaymentGroupResponseForPaymentUser.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        PaymentGroupResponse paymentGroupResponseForAPaymentUser =
            getPaymentGroupResponseForPaymentUser.getBody().as(PaymentGroupResponse.class);
        verifyThePaymentGroupResponseForPayments(serviceRequestDto, paymentGroupResponseForAPaymentUser,true);

        Response getPaymentGroupResponseForSolicitorUser =
            serviceRequestTestService
                .getPaymentGroups(USER_TOKEN_PUI_CASE_MANAGER, SERVICE_TOKEN, serviceRequestDto.getCcdCaseNumber());
        assertThat(getPaymentGroupResponseForSolicitorUser.getStatusCode()).isEqualTo(HttpStatus.OK.value());
                PaymentGroupResponse paymentGroupResponseForASolicitorUser =
            getPaymentGroupResponseForSolicitorUser.getBody().as(PaymentGroupResponse.class);
        verifyThePaymentGroupResponseForPayments(serviceRequestDto, paymentGroupResponseForASolicitorUser,true);
    }

    @Test
    public void negative_create_service_request_and_an_overpayment_pba_payment_user_hmcts() throws Exception {

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
    // @Ignore("Test Build")
    // @Ignore("TODO - The Response Body is not of a proper MIME Type....")
    public void negative_create_service_request_and_an_underpayment_pba_payment_user_hmcts() throws Exception {
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
    // @Ignore("Test Build")
    public void positive_create_service_request_and_a_pba_payment_and_a_duplicate_payment_for_same_idempotent_key()
        throws Exception {

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
    // @Ignore("Test Build")
    public void positive_create_service_request_and_a_pba_payment_and_a_duplicate_payment_for_a_different_idempotent_Key()
        throws Exception {

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
    // @Ignore("Test Build")
    public void positive_create_service_request_and_a_duplicate_service_request_post_failed_payment_account_deleted()
        throws Exception {

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
    // @Ignore("Test Build")
    public void positive_create_service_request_and_a_duplicate_service_request_post_failed_payment_account_on_hold()
        throws Exception {

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
    // @Ignore("Test Build")
    public void positive_create_service_request_and_a_duplicate_service_request_post_failed_payment_account_over_limit()
        throws Exception {

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
//    @Ignore ("Card payment Failing on the amounts Decimal Point......")
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
            .build();
        Response createOnlineCardPaymentResponse =
            serviceRequestTestService.createAnOnlineCardPaymentForAServiceRequest(USER_TOKEN_PAYMENT,
                SERVICE_TOKEN, serviceRequestReference, onlineCardPaymentRequest);
        assertThat(createOnlineCardPaymentResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        OnlineCardPaymentResponse onlineCardPaymentResponse =
            createOnlineCardPaymentResponse.getBody().as(OnlineCardPaymentResponse.class);
        assertThat(onlineCardPaymentResponse.getPaymentReference()).matches(PAYMENTS_REGEX_PATTERN);
        assertThat(onlineCardPaymentResponse.getNextUrl()).isNotNull();
        assertThat(onlineCardPaymentResponse.getNextUrl()).isNotBlank();
        assertThat(onlineCardPaymentResponse.getNextUrl()).startsWith("https://");

        Response getPaymentGroupResponseForPaymentUser =
            serviceRequestTestService.getPaymentGroups(USER_TOKEN_PAYMENT, SERVICE_TOKEN, serviceRequestDto.getCcdCaseNumber());
        assertThat(getPaymentGroupResponseForPaymentUser.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        PaymentGroupResponse paymentGroupResponseForAPaymentUser =
            getPaymentGroupResponseForPaymentUser.getBody().as(PaymentGroupResponse.class);
        verifyThePaymentGroupResponseForPayments(serviceRequestDto, paymentGroupResponseForAPaymentUser,false);

        Response getPaymentGroupResponseForSolicitorUser =
            serviceRequestTestService
                .getPaymentGroups(USER_TOKEN_PUI_ORGANISATION_MANAGER, SERVICE_TOKEN, serviceRequestDto.getCcdCaseNumber());
        assertThat(getPaymentGroupResponseForSolicitorUser.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        PaymentGroupResponse paymentGroupResponseForASolicitorUser =
            getPaymentGroupResponseForSolicitorUser.getBody().as(PaymentGroupResponse.class);
        verifyThePaymentGroupResponseForNoPaymentsOrRemisssions(serviceRequestDto, paymentGroupResponseForASolicitorUser);
    }

    @Test
    //@Ignore("Test Build")
    public void positive_create_service_request_and_an_underpayment_full_card_payment_user_hmcts() throws Exception {

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
    //@Ignore("Test Build")
    public void positive_create_service_request_and_an_overpayment_full_card_payment_user_hmcts() throws Exception {

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
    public void negative_get_service_request_for_invalid_ccd_case_number() throws Exception {
        Response getPaymentGroupResponse =
            serviceRequestTestService.getPaymentGroups(USER_TOKEN_PAYMENT, SERVICE_TOKEN, "Test-Invalid");
        assertThat(getPaymentGroupResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    //@Ignore ("Should not be able to Create a Service Request with an Invalid hmctsOrgID")
    public void negative_create_service_request_for_invalid_hmcts_org_id() throws Exception {
        ServiceRequestDto serviceRequestDto
            = ServiceRequestFixture.buildServiceRequestDTO("ZAM6", null);
        Response createServiceRequestResponse
            = serviceRequestTestService.createServiceRequest(USER_TOKEN_PAYMENT, SERVICE_TOKEN,
            serviceRequestDto);
        assertThat(createServiceRequestResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(createServiceRequestResponse.getBody().asString()).isEqualTo("No Service found for given CaseType or HMCTS Org Id");
    }

    private void verifyThePaymentGroupResponseForNoPaymentsOrRemisssions(final ServiceRequestDto serviceRequestDto,
                                                                         final PaymentGroupResponse paymentGroupResponse)
        throws Exception {
        paymentGroupResponse.getPaymentGroups().stream().forEach(paymentGroupDto -> {
            assertThat(paymentGroupDto.getPayments()).isNull();
            assertThat(paymentGroupDto.getRemissions()).isNullOrEmpty();
            System.out
                .println("The value of the Status : " + paymentGroupResponse.getPaymentGroups().get(0).getServiceRequestStatus());
            assertThat(paymentGroupDto.getPaymentGroupReference()).matches(SERVICE_REQUEST_REGEX_PATTERN);
            assertThat(paymentGroupDto.getFees().get(0).getCode().equals(serviceRequestDto.getFees().get(0).getCode())).isTrue();
            assertThat(paymentGroupDto.getServiceRequestStatus().equals(NOT_PAID)).isTrue();
            assertThat(paymentGroupDto.getDateCreated()).isNotNull();
            System.out.println("The value of the Date Created" + paymentGroupDto.getDateCreated());
            try {
                verifyDate(paymentGroupDto.getDateCreated());
                verifyDate(paymentGroupDto.getDateUpdated());
            } catch (ParseException e) {
                e.printStackTrace();
                fail("The Date Check has failed....");
            }
        });
    }

    private static void verifyThePaymentGroupResponseForPayments(final ServiceRequestDto serviceRequestDto,
                                                                 final PaymentGroupResponse paymentGroupResponse,
                                                                 final boolean paymentStatusFlag)
        throws Exception {
        paymentGroupResponse.getPaymentGroups().stream().forEach(paymentGroupDto -> {
            assertThat(paymentGroupDto.getPayments()).isNotNull();
            assertThat(paymentGroupDto.getPayments().get(0).getAmount())
                .isEqualTo(serviceRequestDto.getFees().get(0).getCalculatedAmount());
            assertThat(paymentGroupDto.getPayments().get(0).getCcdCaseNumber()).isEqualTo(serviceRequestDto.getCcdCaseNumber());
            assertThat(paymentGroupDto.getRemissions().size()).isEqualTo(0);
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
        System.out.println("The body of the message : " + message.getBody().toString());
    }
}
