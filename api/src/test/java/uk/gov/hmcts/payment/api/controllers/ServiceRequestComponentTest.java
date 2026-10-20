package uk.gov.hmcts.payment.api.controllers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import uk.gov.hmcts.payment.api.domain.service.ServiceRequestCancellationEligibilityService;
import uk.gov.hmcts.payment.api.domain.service.ServiceRequestCancellationEligibilityService.Eligibility;
import uk.gov.hmcts.payment.api.dto.servicerequest.ServiceRequestCancelDto;
import uk.gov.hmcts.payment.api.jpaaudit.model.AuditEventsType;
import uk.gov.hmcts.payment.api.jpaaudit.model.PaymentAuditHistory;
import uk.gov.hmcts.payment.api.jpaaudit.model.PaymentAuditHistoryRepository;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.service.ReferenceDataService;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Unlike ServiceRequestControllerTest, this test uses a real ServiceRequestDomainService
 * and makes writes to the db
 */

@RunWith(SpringRunner.class)
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
public class ServiceRequestComponentTest {

    private static final String USER_ID = UserResolverBackdoor.CASEWORKER_ID;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

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

    @MockitoBean
    // Reference-data is mocked because cancellation does not use it. The domain service, controller,
    // repository, Liquibase schema and database are real in this component test.
    private ReferenceDataService referenceDataService;

    @MockitoBean
    private ServiceRequestCancellationEligibilityService cancellationEligibilityService;

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
        when(cancellationEligibilityService.checkEligibility(any())).thenReturn(Eligibility.eligible());
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

        manualCancellationRestActions().post("/service-request/" + serviceRequestReference + "/cancel", cancellation)
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNoContent());

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
        String cancellationPath = "/service-request/" + serviceRequestReference + "/cancel";

        serviceOnlyActions.post(cancellationPath)
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNoContent());
        PaymentFeeLink cancelled = paymentFeeLinkRepository.findByPaymentReference(serviceRequestReference)
            .orElseThrow();
        var cancellationDate = cancelled.getDateCancelled();

        serviceOnlyActions.post(cancellationPath)
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNoContent());

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
        when(cancellationEligibilityService.checkEligibility(any()))
            .thenReturn(Eligibility.ineligible(ineligibleReason));

        serviceOnlyRestActions()
            .post("/service-request/" + serviceRequestReference + "/cancel")
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isConflict())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$").value(ineligibleReason));

        PaymentFeeLink unchanged = paymentFeeLinkRepository.findByPaymentReference(serviceRequestReference)
            .orElseThrow();
        assertThat(unchanged.isCancelled()).isFalse();
        assertThat(unchanged.getCancellationReason()).isNull();
        assertThat(unchanged.getCancelledBy()).isNull();
        assertThat(unchanged.getDateCancelled()).isNull();
        assertThat(paymentAuditHistoryRepository.findByCcdCaseNo(ccdCaseNumber))
            .noneMatch(entry -> AuditEventsType.ORDER_CANCELLED.getName().equals(entry.getAuditType().getName()));
        verify(cancellationEligibilityService).checkEligibility(any());
    }

    @Test
    public void cancelServiceRequestReturnsNotFoundForUnknownReference() throws Exception {
        String serviceRequestReference = newServiceRequestReference();

        serviceOnlyRestActions()
            .post("/service-request/" + serviceRequestReference + "/cancel")
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
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
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());

        newRestActions()
            .post("/service-request/2026-12345678901/cancel")
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());

        newRestActions()
            .withAuthorizedService("divorce")
            .withHeader("Authorization", "invalid-user-token")
            .post("/service-request/2026-12345678901/cancel")
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    public void manualCancellationRequiresPaymentsRole() throws Exception {
        newRestActions()
            .withAuthorizedService("divorce")
            .withAuthorizedUser(UserResolverBackdoor.CITIZEN_ID)
            .post("/service-request/2026-12345678901/cancel", ServiceRequestCancelDto.cancelDtoWith()
                .reason("The service request was created in error")
                .build())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
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
        return "2026-" + ThreadLocalRandom.current().nextLong(1_000_000_000_000L, 10_000_000_000_000L);
    }

    private RestActions newRestActions() {
        return new RestActions(
            webAppContextSetup(webApplicationContext).apply(springSecurity()).build(),
            serviceRequestAuthorizer, userRequestAuthorizer, objectMapper
        );
    }

    @Test
    public void cancellationRowLockBlocksAnotherTransactionUntilCommit() throws Exception {
        String serviceRequestReference = newServiceRequestReference();
        paymentFeeLinkRepository.save(PaymentFeeLink.paymentFeeLinkWith()
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
                return paymentFeeLinkRepository.findByPaymentReferenceForUpdate(serviceRequestReference)
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
