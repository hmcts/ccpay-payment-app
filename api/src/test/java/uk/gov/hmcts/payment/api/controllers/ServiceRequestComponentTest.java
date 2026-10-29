package uk.gov.hmcts.payment.api.controllers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.configuration.LaunchDarklyFeatureToggler;
import uk.gov.hmcts.payment.api.contract.util.CurrencyCode;
import uk.gov.hmcts.payment.api.domain.service.ServiceRequestCancellationEligibilityService;
import uk.gov.hmcts.payment.api.domain.service.ServiceRequestCancellationEligibilityService.Eligibility;
import uk.gov.hmcts.payment.api.dto.AccountDto;
import uk.gov.hmcts.payment.api.dto.OnlineCardPaymentRequest;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.PaymentGroupResponse;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestCancelDto;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestPaymentDto;
import uk.gov.hmcts.payment.api.external.client.dto.CreatePaymentRequest;
import uk.gov.hmcts.payment.api.external.client.dto.GovPayPayment;
import uk.gov.hmcts.payment.api.external.client.dto.Link;
import uk.gov.hmcts.payment.api.external.client.dto.State;
import uk.gov.hmcts.payment.api.jpaaudit.model.AuditEventsType;
import uk.gov.hmcts.payment.api.jpaaudit.model.PaymentAuditHistory;
import uk.gov.hmcts.payment.api.jpaaudit.model.PaymentAuditHistoryRepository;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.reports.FeesRegisterClient;
import uk.gov.hmcts.payment.api.service.AccountService;
import uk.gov.hmcts.payment.api.service.DelegatingPaymentService;
import uk.gov.hmcts.payment.api.service.PaymentRefundsService;
import uk.gov.hmcts.payment.api.service.ReferenceDataService;
import uk.gov.hmcts.payment.api.service.RefundRemissionEnableService;
import uk.gov.hmcts.payment.api.servicebus.TopicClientProxy;
import uk.gov.hmcts.payment.api.util.AccountStatus;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * This test uses a real ServiceRequestDomainService
 * and makes writes to the db (unlike ServiceRequestControllerTest which uses mocks)
 * <p>
 * These tests let the application's transactions commit so they can verify persisted changes.
 * The committed test data is not rolled back automatically. Sharing a database with other test
 * classes allowed those records to appear in their queries and made results depend on test order.
 * The distinct Testcontainers JDBC URL gives this class its own PostgreSQL container and database,
 * so it can verify committed changes without affecting other component test classes.
 *
 * @author jhilling
 */
@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK,
    properties = "spring.datasource.url=jdbc:tc:postgresql:11.0://localhost/service_request_component_tests")
public class ServiceRequestComponentTest {

    private static final String USER_ID = UserResolverBackdoor.CASEWORKER_ID;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    @Autowired
    private Payment2Repository paymentRepository;

    @Autowired
    private PaymentAuditHistoryRepository paymentAuditHistoryRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    private UserResolverBackdoor userRequestAuthorizer;

    @Autowired
    private ObjectMapper objectMapper;

    // Reference-data is mocked because cancellation does not use it. The domain service, controller,
    // repository, Liquibase schema and database are real in this component test.
    @MockitoBean
    private ReferenceDataService<?> referenceDataService;

    @MockitoBean
    private ServiceRequestCancellationEligibilityService cancellationEligibilityService;

    @MockitoBean
    private DelegatingPaymentService<GovPayPayment, String> delegateGovPay;

    @MockitoBean
    private AccountService<AccountDto, String> accountService;

    // Keep feature flags and display-only dependencies local to these payment tests.
    @MockitoBean
    private LaunchDarklyFeatureToggler featureToggler;

    @MockitoBean
    private FeesRegisterClient feesRegisterClient;

    @MockitoBean
    private RefundRemissionEnableService refundRemissionEnableService;

    // The case payment groups endpoint asks the refunds app about refunds and remissions.
    @MockitoBean
    private PaymentRefundsService paymentRefundsService;

    private RestActions restActions;

    @Before
    public void setup() {
        MockMvc mvc = webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();

        restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper)
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID);

        when(referenceDataService.getOrganisationalDetail(any(), any(), any())).thenReturn(null);
        when(cancellationEligibilityService.checkEligibility(any(), any())).thenReturn(Eligibility.eligible());
        when(paymentRefundsService.checkRefundAgainstRemissionV2(any(), any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(1));
    }

    @Test
    public void cancelServiceRequestUpdatesTheDatabase() throws Exception {
        String serviceRequestReference = newServiceRequestReference();
        String ccdCaseNumber = UUID.randomUUID().toString().replace("-", "");
        String reason = "The service request was created in error";

        paymentFeeLinkRepository.save(PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference(serviceRequestReference)
            .ccdCaseNumber(ccdCaseNumber)
            .build());

        ServiceRequestCancelDto cancellation = ServiceRequestCancelDto.cancelDtoWith()
            .reason(reason)
            .build();

        manualCancellationRestActions().post("/service-request/%s/cancel".formatted(serviceRequestReference), cancellation)
            .andExpect(status().isNoContent());

        PaymentFeeLink persisted = paymentFeeLinkRepository.findByPaymentReference(serviceRequestReference)
            .orElseThrow();

        assertThat(persisted.isCancelled()).isTrue();
        assertThat(persisted.getCancellationReason()).isEqualTo(reason);
        assertThat(persisted.getCancelledBy()).isEqualTo(USER_ID);
        assertThat(persisted.getDateCancelled()).isNotNull();

        List<PaymentAuditHistory> cancellationAuditEntries = paymentAuditHistoryRepository.findByCcdCaseNo(ccdCaseNumber).stream()
            .filter(entry -> AuditEventsType.ORDER_CANCELLED.getName().equals(entry.getAuditType().getName()))
            .toList();

        assertThat(cancellationAuditEntries).hasSize(1);
        PaymentAuditHistory auditEntry = cancellationAuditEntries.getFirst();
        assertThat(auditEntry.getAuditType().getName()).isEqualTo(AuditEventsType.ORDER_CANCELLED.getName());
        assertThat(auditEntry.getAuditDescription()).isEqualTo(AuditEventsType.ORDER_CANCELLED.getDescription());
        assertThat(jdbcTemplate.queryForObject(
            "select description from audit_events_type where name = ?", String.class, AuditEventsType.ORDER_CANCELLED.getName()))
            .as("Liquibase audit_events_type description should match AuditEventsType.ORDER_CANCELLED")
            .isEqualTo(AuditEventsType.ORDER_CANCELLED.getDescription());
        assertThat(auditEntry.getAuditPayload()).contains(reason);
    }

    @Test
    public void automatedCancellationUsesDefaultReasonAndIsIdempotent() throws Exception {
        String serviceRequestReference = newServiceRequestReference();
        String ccdCaseNumber = UUID.randomUUID().toString().replace("-", "");
        paymentFeeLinkRepository.save(PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference(serviceRequestReference)
            .ccdCaseNumber(ccdCaseNumber)
            .build());

        RestActions serviceOnlyActions = serviceOnlyRestActions();
        String cancellationPath = "/service-request/%s/cancel".formatted(serviceRequestReference);

        serviceOnlyActions.post(cancellationPath)
            .andExpect(status().isNoContent());
        PaymentFeeLink cancelled = paymentFeeLinkRepository.findByPaymentReference(serviceRequestReference)
            .orElseThrow();
        var cancellationDate = cancelled.getDateCancelled();

        serviceOnlyActions.post(cancellationPath)
            .andExpect(status().isNoContent());

        PaymentFeeLink persisted = paymentFeeLinkRepository.findByPaymentReference(serviceRequestReference)
            .orElseThrow();
        assertThat(persisted.isCancelled()).isTrue();
        assertThat(persisted.getCancellationReason()).isEqualTo("Auto-Cancelled by Service");
        assertThat(persisted.getCancelledBy()).isEqualTo("divorce");
        assertThat(persisted.getDateCancelled()).isEqualTo(cancellationDate);
        List<PaymentAuditHistory> cancellationAuditEntries = paymentAuditHistoryRepository.findByCcdCaseNo(ccdCaseNumber).stream()
            .filter(entry -> AuditEventsType.ORDER_CANCELLED.getName().equals(entry.getAuditType().getName()))
            .toList();
        assertThat(cancellationAuditEntries).hasSize(1);
        assertThat(cancellationAuditEntries.getFirst().getAuditPayload()).contains("Auto-Cancelled by Service");
    }

    @Test
    public void ineligibleServiceRequestReturnsConflictAndRemainsUnchanged() throws Exception {
        String serviceRequestReference = newServiceRequestReference();
        String ccdCaseNumber = UUID.randomUUID().toString().replace("-", "");
        paymentFeeLinkRepository.save(PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference(serviceRequestReference)
            .ccdCaseNumber(ccdCaseNumber)
            .build());
        String ineligibleReason = "Service request has a successful payment RC-1234-1234-1234";
        when(cancellationEligibilityService.checkEligibility(any(), any()))
            .thenReturn(Eligibility.ineligible(ineligibleReason));

        serviceOnlyRestActions()
            .post("/service-request/%s/cancel".formatted(serviceRequestReference))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").value(ineligibleReason));

        PaymentFeeLink unchanged = paymentFeeLinkRepository.findByPaymentReference(serviceRequestReference)
            .orElseThrow();
        assertThat(unchanged.isCancelled()).isFalse();
        assertThat(unchanged.getCancellationReason()).isNull();
        assertThat(unchanged.getCancelledBy()).isNull();
        assertThat(unchanged.getDateCancelled()).isNull();
        assertThat(paymentAuditHistoryRepository.findByCcdCaseNo(ccdCaseNumber))
            .noneMatch(entry -> AuditEventsType.ORDER_CANCELLED.getName().equals(entry.getAuditType().getName()));
        verify(cancellationEligibilityService).checkEligibility(any(), any());
    }

    /**
     * Test that a service request cancelled through the API is shown as cancelled by the applicable read endpoints
     */
    @Test
    public void cancelledServiceRequestIsReturnedAsCancelledByReadEndpoints() throws Exception {
        PaymentFeeLink serviceRequest = saveUnpaidServiceRequest();
        String serviceRequestReference = serviceRequest.getPaymentReference();
        String reason = "The service request was created in error";

        manualCancellationRestActions().post("/service-request/%s/cancel".formatted(serviceRequestReference),
                ServiceRequestCancelDto.cancelDtoWith().reason(reason).build())
            .andExpect(status().isNoContent());

        Date dateCancelled = paymentFeeLinkRepository.findByPaymentReference(serviceRequestReference)
            .orElseThrow()
            .getDateCancelled();

        String paymentGroupJson = restActions.get("/payment-groups/%s".formatted(serviceRequestReference))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.service_request_status").value("Cancelled"))
            .andExpect(jsonPath("$.cancelled").value(true))
            .andExpect(jsonPath("$.cancellation_reason").value(reason))
            .andExpect(jsonPath("$.cancelled_by").value(USER_ID))
            .andExpect(jsonPath("$.date_cancelled").exists())
            .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readValue(paymentGroupJson, PaymentGroupDto.class).getDateCancelled())
            .isEqualTo(dateCancelled);

        String casePaymentGroupsJson = restActions.get("/cases/%s/paymentgroups".formatted(serviceRequest.getCcdCaseNumber()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.payment_groups.length()").value(1))
            .andExpect(jsonPath("$.payment_groups[0].payment_group_reference").value(serviceRequestReference))
            .andExpect(jsonPath("$.payment_groups[0].service_request_status").value("Cancelled"))
            .andExpect(jsonPath("$.payment_groups[0].cancelled").value(true))
            .andExpect(jsonPath("$.payment_groups[0].cancellation_reason").value(reason))
            .andExpect(jsonPath("$.payment_groups[0].cancelled_by").value(USER_ID))
            .andExpect(jsonPath("$.payment_groups[0].date_cancelled").exists())
            .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readValue(casePaymentGroupsJson, PaymentGroupResponse.class).getPaymentGroups())
            .singleElement()
            .satisfies(paymentGroup -> assertThat(paymentGroup.getDateCancelled()).isEqualTo(dateCancelled));
    }

    /**
     * Test that a service request that has not been cancelled keeps its normal status in the read endpoints
     * and leaves out the cancellation fields, so existing consumers see no change in the response.
     */
    @Test
    public void openServiceRequestOmitsCancellationFieldsFromReadEndpoints() throws Exception {
        PaymentFeeLink serviceRequest = saveUnpaidServiceRequest();

        restActions.get("/payment-groups/%s".formatted(serviceRequest.getPaymentReference()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.service_request_status").value("Not paid"))
            .andExpect(jsonPath("$.cancelled").doesNotExist())
            .andExpect(jsonPath("$.cancellation_reason").doesNotExist())
            .andExpect(jsonPath("$.cancelled_by").doesNotExist())
            .andExpect(jsonPath("$.date_cancelled").doesNotExist());

        restActions.get("/cases/%s/paymentgroups".formatted(serviceRequest.getCcdCaseNumber()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.payment_groups[0].service_request_status").value("Not paid"))
            .andExpect(jsonPath("$.payment_groups[0].cancelled").doesNotExist())
            .andExpect(jsonPath("$.payment_groups[0].cancellation_reason").doesNotExist())
            .andExpect(jsonPath("$.payment_groups[0].cancelled_by").doesNotExist())
            .andExpect(jsonPath("$.payment_groups[0].date_cancelled").doesNotExist());
    }

    /**
     * Test that an unpaid service request that has not been cancelled can still be paid by PBA.
     * The payment is saved in the database, and repeating the same request returns the original
     * payment without creating another payment, checking the account again or sending another callback.
     */
    @Test
    public void pbaPaymentForOpenServiceRequestIsPersistedAndReplayedWithoutAddingAnotherPayment() throws Exception {
        PaymentFeeLink serviceRequest = saveUnpaidServiceRequest();
        String accountNumber = "PBA12345";
        when(accountService.retrieve(accountNumber)).thenReturn(AccountDto.accountDtoWith()
            .accountNumber(accountNumber)
            .status(AccountStatus.ACTIVE)
            .availableBalance(new BigDecimal("100.00"))
            .build());

        ServiceRequestPaymentDto paymentRequest = ServiceRequestPaymentDto.paymentDtoWith()
            .amount(new BigDecimal("10.00"))
            .currency("GBP")
            .customerReference("component-test")
            .accountNumber(accountNumber)
            .idempotencyKey(UUID.randomUUID().toString())
            .organisationName("component-test")
            .build();
        String endpoint = "/service-request/%s/pba-payments".formatted(serviceRequest.getPaymentReference());

        // The service constructs its callback client directly; intercept only this external boundary.
        try (MockedConstruction<TopicClientProxy> callbackClients = mockConstruction(TopicClientProxy.class)) {
            String responseBody = serviceOnlyRestActions()
                .post(endpoint, paymentRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.payment_reference").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
            String paymentReference = objectMapper.readTree(responseBody).get("payment_reference").asText();

            serviceOnlyRestActions()
                .post(endpoint, paymentRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.payment_reference").value(paymentReference));

            Payment persisted = paymentRepository.findByReference(paymentReference).orElseThrow();
            assertThat(persisted.getAmount()).isEqualByComparingTo("10.00");
            assertThat(persisted.getCurrency()).isEqualTo("GBP");
            assertThat(persisted.getPaymentStatus().getName()).isEqualTo("success");
            assertThat(persisted.getPaymentMethod().getName()).isEqualTo("payment by account");
            assertThat(persisted.getPbaNumber()).isEqualTo(accountNumber);
            assertThat(persisted.getPaymentLink().getId()).isEqualTo(serviceRequest.getId());
            assertThat(jdbcTemplate.queryForObject(
                "select count(*) from payment where payment_link_id = ?", Integer.class, serviceRequest.getId()))
                .isEqualTo(1);
            assertThat(paymentFeeLinkRepository.findByPaymentReference(serviceRequest.getPaymentReference())
                .orElseThrow().isCancelled()).isFalse();
            verify(accountService).retrieve(accountNumber);
            assertThat(callbackClients.constructed()).hasSize(1);
            verify(callbackClients.constructed().getFirst()).send(any());
        }
    }

    /**
     * Test that PBA payments cannot be added to a cancelled SR
     */
    @Test
    public void pbaPaymentForCancelledServiceRequestReturnsAndReplaysConflictWithoutAddingPayment() throws Exception {
        String serviceRequestReference = newServiceRequestReference();
        String message = "Service request cancelled";
        String idempotencyKey = UUID.randomUUID().toString();
        paymentFeeLinkRepository.save(PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference(serviceRequestReference)
            .cancelled(true)
            .build());

        ServiceRequestPaymentDto paymentRequest = ServiceRequestPaymentDto.paymentDtoWith()
            .amount(new java.math.BigDecimal("10.00"))
            .currency("GBP")
            .customerReference("component-test")
            .accountNumber("PBA12345")
            .idempotencyKey(idempotencyKey)
            .organisationName("component-test")
            .build();
        String endpoint = "/service-request/%s/pba-payments".formatted(serviceRequestReference);

        serviceOnlyRestActions()
            .post(endpoint, paymentRequest)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$").value(message));

        serviceOnlyRestActions()
            .post(endpoint, paymentRequest)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$").value(message));

        PaymentFeeLink unchanged = paymentFeeLinkRepository.findByPaymentReference(serviceRequestReference)
            .orElseThrow();
        assertThat(unchanged.isCancelled()).isTrue();
        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from payment where payment_link_id = ?", Integer.class, unchanged.getId()))
            .isZero();
        verifyNoInteractions(accountService);
    }

    /**
     * Test that an unpaid service request that has not been cancelled can still start a card payment.
     * The request is sent to the mocked GovPay provider, the payment is saved against the service
     * request in the database, and the response gives the caller the next URL to continue payment.
     */
    @Test
    public void cardPaymentForOpenServiceRequestIsPersisted() throws Exception {
        PaymentFeeLink serviceRequest = saveUnpaidServiceRequest();
        String externalReference = UUID.randomUUID().toString();
        String nextUrl = "https://example.com/govpay/next";
        when(delegateGovPay.create(any(CreatePaymentRequest.class), eq(serviceRequest.getEnterpriseServiceName())))
            .thenReturn(GovPayPayment.govPaymentWith()
                .amount(1000)
                .paymentId(externalReference)
                .state(State.stateWith().status("created").finished(false).build())
                .links(GovPayPayment.Links.linksWith().nextUrl(Link.linkWith().href(nextUrl).build()).build())
                .build());

        OnlineCardPaymentRequest paymentRequest = OnlineCardPaymentRequest.onlineCardPaymentRequestWith()
            .amount(new BigDecimal("10.00"))
            .currency(CurrencyCode.GBP)
            .language("en")
            .returnUrl("https://example.com/payment-return")
            .build();

        String responseBody = serviceOnlyRestActions()
            .post("/service-request/%s/card-payments".formatted(serviceRequest.getPaymentReference()), paymentRequest)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("Initiated"))
            .andExpect(jsonPath("$.external_reference").value(externalReference))
            .andExpect(jsonPath("$.next_url").value(nextUrl))
            .andExpect(jsonPath("$.payment_reference").isNotEmpty())
            .andReturn().getResponse().getContentAsString();
        String paymentReference = objectMapper.readTree(responseBody).get("payment_reference").asText();

        Payment persisted = paymentRepository.findByReference(paymentReference).orElseThrow();
        assertThat(persisted.getAmount()).isEqualByComparingTo("10.00");
        assertThat(persisted.getCurrency()).isEqualTo("GBP");
        assertThat(persisted.getPaymentStatus().getName()).isEqualTo("created");
        assertThat(persisted.getPaymentMethod().getName()).isEqualTo("card");
        assertThat(persisted.getExternalReference()).isEqualTo(externalReference);
        assertThat(persisted.getPaymentLink().getId()).isEqualTo(serviceRequest.getId());
        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from payment where payment_link_id = ?", Integer.class, serviceRequest.getId()))
            .isEqualTo(1);
        assertThat(paymentFeeLinkRepository.findByPaymentReference(serviceRequest.getPaymentReference())
            .orElseThrow().isCancelled()).isFalse();

        ArgumentCaptor<CreatePaymentRequest> govPayRequest = ArgumentCaptor.forClass(CreatePaymentRequest.class);
        verify(delegateGovPay).create(govPayRequest.capture(), eq(serviceRequest.getEnterpriseServiceName()));
        assertThat(govPayRequest.getValue().getAmount()).isEqualTo(1000);
        assertThat(govPayRequest.getValue().getReference()).isEqualTo(paymentReference);
    }

    /**
     * Test that a cancelled service request cannot start a card payment.
     * The response returns HTTP 409 with a message explaining that the service request is cancelled.
     * No payment is saved or request sent to GovPay, and the service request remains cancelled.
     */
    @Test
    public void cardPaymentForCancelledServiceRequestReturnsConflictWithoutAddingPayment() throws Exception {
        String serviceRequestReference = newServiceRequestReference();
        String message = "Service request cancelled";
        paymentFeeLinkRepository.save(PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference(serviceRequestReference)
            .cancelled(true)
            .build());

        OnlineCardPaymentRequest paymentRequest = OnlineCardPaymentRequest.onlineCardPaymentRequestWith()
            .amount(new java.math.BigDecimal("10.00"))
            .currency(CurrencyCode.GBP)
            .language("en")
            .returnUrl("https://example.com/payment-return")
            .build();

        serviceOnlyRestActions()
            .post("/service-request/%s/card-payments".formatted(serviceRequestReference), paymentRequest)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$").value(message));

        PaymentFeeLink unchanged = paymentFeeLinkRepository.findByPaymentReference(serviceRequestReference)
            .orElseThrow();
        assertThat(unchanged.isCancelled()).isTrue();
        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from payment where payment_link_id = ?", Integer.class, unchanged.getId()))
            .isZero();
        verifyNoInteractions(delegateGovPay);
    }

    @Test
    public void cancelServiceRequestReturnsNotFoundForUnknownReference() throws Exception {
        String serviceRequestReference = newServiceRequestReference();

        serviceOnlyRestActions()
            .post("/service-request/%s/cancel".formatted(serviceRequestReference))
            .andExpect(status().isNotFound())
            .andExpect(content()
                .string("Service request not found"));
    }

    @Test
    public void cancellationRequiresValidServiceAuthorization() throws Exception {
        RestActions unauthorizedActions = new RestActions(
            webAppContextSetup(webApplicationContext).apply(springSecurity()).build(),
            serviceRequestAuthorizer, userRequestAuthorizer, objectMapper
        ).withAuthorizedService("unauthorizedService");

        unauthorizedActions
            .post("/service-request/2026-12345678901/cancel")
            .andExpect(status().isForbidden());

        newRestActions()
            .post("/service-request/2026-12345678901/cancel")
            .andExpect(status().isForbidden());

        newRestActions()
            .withAuthorizedService("divorce")
            .withHeader("Authorization", "invalid-user-token")
            .post("/service-request/2026-12345678901/cancel")
            .andExpect(status().isForbidden());
    }

    @Test
    public void manualCancellationRequiresPaymentsRole() throws Exception {
        newRestActions()
            .withAuthorizedService("divorce")
            .withAuthorizedUser(UserResolverBackdoor.CITIZEN_ID)
            .post("/service-request/2026-12345678901/cancel", ServiceRequestCancelDto.cancelDtoWith()
                .reason("The service request was created in error")
                .build())
            .andExpect(status().isForbidden());
    }

    private PaymentFeeLink saveUnpaidServiceRequest() {
        BigDecimal amount = new BigDecimal("10.00");
        // The case payment groups search matches on the fee's case number, so it is set on both.
        String ccdCaseNumber = UUID.randomUUID().toString().replace("-", "");
        return paymentFeeLinkRepository.save(PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference(newServiceRequestReference())
            .enterpriseServiceName("Divorce")
            .orgId("AA01")
            .ccdCaseNumber(ccdCaseNumber)
            .fees(List.of(PaymentFee.feeWith()
                .ccdCaseNumber(ccdCaseNumber)
                .code("FEE0001")
                .version("1")
                .volume(1)
                .calculatedAmount(amount)
                .amountDue(amount)
                .feeAmount(amount)
                .netAmount(amount)
                .build()))
            .build());
    }

    private RestActions serviceOnlyRestActions() {
        return newRestActions().withAuthorizedService("divorce");
    }

    private RestActions manualCancellationRestActions() {
        return newRestActions()
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID);
    }

    // Matches generated references: year, dash, then a 13 digit sequence value
    private static String newServiceRequestReference() {
        return "2026-%d".formatted(ThreadLocalRandom.current().nextLong(1_000_000_000_000L, 10_000_000_000_000L));
    }

    private RestActions newRestActions() {
        return new RestActions(
            webAppContextSetup(webApplicationContext).apply(springSecurity()).build(),
            serviceRequestAuthorizer, userRequestAuthorizer, objectMapper
        );
    }

    /**
     * Test that locking a service request by its ID waits for cancellation holding the reference lock.
     * Both lookups must lock the same row, and the waiting transaction must see the committed cancellation.
     */
    @Test
    public void cancellationRowLockBlocksAnotherTransactionUntilCommit() throws Exception {
        String serviceRequestReference = newServiceRequestReference();
        PaymentFeeLink serviceRequest = paymentFeeLinkRepository.save(PaymentFeeLink.paymentFeeLinkWith()
            .paymentReference(serviceRequestReference)
            .build());

        CountDownLatch firstTransactionHasLock = new CountDownLatch(1);
        CountDownLatch secondTransactionHasPid = new CountDownLatch(1);
        AtomicInteger secondBackendPid = new AtomicInteger();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> lockHolder = executor.submit(() -> transactionTemplate.execute(status -> {
                PaymentFeeLink locked = paymentFeeLinkRepository.findByPaymentReferenceForUpdate(serviceRequestReference)
                    .orElseThrow();
                int lockHolderPid = jdbcTemplate.queryForObject("select pg_backend_pid()", Integer.class);
                firstTransactionHasLock.countDown();
                await(secondTransactionHasPid);

                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                boolean secondTransactionBlocked = false;
                while (!secondTransactionBlocked && System.nanoTime() < deadline) {
                    secondTransactionBlocked = Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                        "select ? = any(pg_blocking_pids(?))",
                        Boolean.class,
                        lockHolderPid,
                        secondBackendPid.get()
                    ));
                    if (!secondTransactionBlocked) {
                        sleepBriefly();
                    }
                }
                assertThat(secondTransactionBlocked)
                    .as("Second transaction should be waiting on the cancellation row lock")
                    .isTrue();

                locked.setCancelled(true);
                paymentFeeLinkRepository.save(locked);
                return null;
            }));

            assertThat(firstTransactionHasLock.await(10, TimeUnit.SECONDS))
                .as("First transaction should acquire the row lock")
                .isTrue();

            Future<Boolean> waitingTransaction = executor.submit(() -> transactionTemplate.execute(status -> {
                secondBackendPid.set(jdbcTemplate.queryForObject("select pg_backend_pid()", Integer.class));
                secondTransactionHasPid.countDown();
                return paymentFeeLinkRepository.findByIdForUpdate(serviceRequest.getId())
                    .orElseThrow()
                    .isCancelled();
            }));

            lockHolder.get(15, TimeUnit.SECONDS);
            assertThat(waitingTransaction.get(10, TimeUnit.SECONDS))
                .as("Waiting transaction should see the committed cancelled state")
                .isTrue();
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS))
                .as("Lock-test executor should terminate")
                .isTrue();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(10, TimeUnit.SECONDS))
                .as("Concurrent lock-test transaction should reach the synchronization point")
                .isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while coordinating lock test", e);
        }
    }

    private static void sleepBriefly() {
        try {
            Thread.sleep(25);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while checking PostgreSQL lock state", e);
        }
    }
}
