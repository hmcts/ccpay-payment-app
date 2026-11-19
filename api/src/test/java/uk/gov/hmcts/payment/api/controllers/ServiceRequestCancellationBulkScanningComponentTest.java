package uk.gov.hmcts.payment.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.payment.api.contract.FeeDto;
import uk.gov.hmcts.payment.api.dto.PaymentGroupDto;
import uk.gov.hmcts.payment.api.dto.PaymentStatus;
import uk.gov.hmcts.payment.api.dto.SearchResponse;
import uk.gov.hmcts.payment.api.jpaaudit.model.AuditEventsType;
import uk.gov.hmcts.payment.api.jpaaudit.model.PaymentAuditHistory;
import uk.gov.hmcts.payment.api.jpaaudit.model.PaymentAuditHistoryRepository;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.service.BulkScanningClient;
import uk.gov.hmcts.payment.api.service.RefundRemissionEnableService;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Cancels a service request created the way paybubble creates one, with {@code POST /payment-groups}, through the
 * real controller, domain service, eligibility check and database. Only the bulk scanning client is mocked, and the
 * refund and remission roles the payment group response is decorated with.
 *
 * <p>Such a service request has its CCD case number only on its fees: {@code payment_fee_link.ccd_case_number} is
 * left empty. The bulk scanning check must still look the case up (demo-int, SR 36247, 10 Nov 2026).</p>
 */
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@Transactional
class ServiceRequestCancellationBulkScanningComponentTest {

    private static final String USER_ID = UserResolverBackdoor.CASEWORKER_ID;
    private static final String CCD_CASE_NUMBER = "1794310324094755";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    private UserResolverBackdoor userRequestAuthorizer;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    @Autowired
    private PaymentAuditHistoryRepository paymentAuditHistoryRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private BulkScanningClient bulkScanningClient;

    // The payment group response says whether its fees can be refunded or remitted, which needs the user's service.
    @MockitoBean
    private RefundRemissionEnableService refundRemissionEnableService;

    @BeforeEach
    void systemUserIsConfigured() {
        when(bulkScanningClient.canAuthenticate()).thenReturn(true);
    }

    @Test
    void refusesToCancelWhenBulkScanningHasUnallocatedPaymentsOnTheCaseOfTheFees() throws Exception {
        String serviceRequestReference = createPaymentGroupForCase();
        givenBulkScanningReturns(PaymentStatus.COMPLETE);

        automatedCancellationRestActions().post("/service-request/%s/cancel".formatted(serviceRequestReference))
            .andExpect(status().isConflict())
            .andExpect(content().string("unallocated bulk scan payment existing on case"));

        verify(bulkScanningClient).findPaymentsForCase(CCD_CASE_NUMBER);
        assertThat(serviceRequest(serviceRequestReference).isCancelled()).isFalse();
        assertThat(cancellationAuditEntries()).isEmpty();
    }

    @Test
    void cancelsWhenBulkScanningHasNothingUnallocatedOnTheCaseOfTheFees() throws Exception {
        String serviceRequestReference = createPaymentGroupForCase();
        givenBulkScanningReturns(PaymentStatus.PROCESSED);

        automatedCancellationRestActions().post("/service-request/%s/cancel".formatted(serviceRequestReference))
            .andExpect(status().isNoContent());

        verify(bulkScanningClient).findPaymentsForCase(CCD_CASE_NUMBER);
        assertThat(serviceRequest(serviceRequestReference).isCancelled()).isTrue();
        // The audit row takes the case number from the fees, as the service request has none of its own.
        assertThat(cancellationAuditEntries()).hasSize(1);
    }

    /**
     * Create a service request as paybubble does, and check its case number is only on its fee.
     */
    private String createPaymentGroupForCase() throws Exception {
        PaymentGroupDto request = PaymentGroupDto.paymentGroupDtoWith()
            .fees(List.of(FeeDto.feeDtoWith()
                .calculatedAmount(new BigDecimal("250"))
                .code("FEE312")
                .version("1")
                .volume(1)
                .ccdCaseNumber(CCD_CASE_NUMBER)
                .build()))
            .build();

        byte[] response = userRestActions().post("/payment-groups", request)
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsByteArray();
        String serviceRequestReference = objectMapper.readValue(response, PaymentGroupDto.class).getPaymentGroupReference();

        // The test's transaction spans both requests. Load the service request afresh, as a separate request would,
        // rather than reuse the instance POST /payment-groups built (which has no remissions list, for example).
        entityManager.flush();
        entityManager.clear();

        PaymentFeeLink created = serviceRequest(serviceRequestReference);
        assertThat(created.getCcdCaseNumber()).isNull();
        assertThat(created.getFees()).singleElement()
            .satisfies(fee -> assertThat(fee.getCcdCaseNumber()).isEqualTo(CCD_CASE_NUMBER));
        return serviceRequestReference;
    }

    private void givenBulkScanningReturns(PaymentStatus status) {
        when(bulkScanningClient.findPaymentsForCase(CCD_CASE_NUMBER))
            .thenReturn(Optional.of(SearchResponse.searchResponseWith().allPaymentsStatus(status).build()));
    }

    private PaymentFeeLink serviceRequest(String serviceRequestReference) {
        return paymentFeeLinkRepository.findByPaymentReference(serviceRequestReference).orElseThrow();
    }

    private List<PaymentAuditHistory> cancellationAuditEntries() {
        return paymentAuditHistoryRepository.findByCcdCaseNo(CCD_CASE_NUMBER).stream()
            .filter(entry -> AuditEventsType.ORDER_CANCELLED.getName().equals(entry.getAuditType().getName()))
            .toList();
    }

    private RestActions userRestActions() {
        return newRestActions()
            .withAuthorizedService("divorce")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID);
    }

    // An automated cancellation: the calling service sends only its S2S token (ServiceAuthorization), with no user
    // Authorization header.
    private RestActions automatedCancellationRestActions() {
        return newRestActions().withAuthorizedService("divorce");
    }

    private RestActions newRestActions() {
        return new RestActions(
            webAppContextSetup(webApplicationContext).apply(springSecurity()).build(),
            serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);
    }
}
