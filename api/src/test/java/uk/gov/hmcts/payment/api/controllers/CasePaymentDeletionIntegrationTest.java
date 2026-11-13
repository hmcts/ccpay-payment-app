package uk.gov.hmcts.payment.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.DockerClientFactory;
import uk.gov.hmcts.payment.api.componenttests.PaymentDbBackdoor;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.Payment2Repository;
import uk.gov.hmcts.payment.api.model.PaymentAuditDeletion;
import uk.gov.hmcts.payment.api.model.PaymentAuditDeletionRepository;
import uk.gov.hmcts.payment.api.model.PaymentChannel;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentMethod;
import uk.gov.hmcts.payment.api.model.PaymentStatus;
import uk.gov.hmcts.payment.api.service.BarPaymentDeletionService;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.payment.api.v1.componenttests.sugar.RestActions;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@EnabledIf("isDockerAvailable")
@ActiveProfiles({"local", "componenttest"})
@SpringBootTest(webEnvironment = MOCK)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
class CasePaymentDeletionIntegrationTest {

    private static final String CCD_CASE_NUMBER = "1747738470383168";

    static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable ex) {
            return false;
        }
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    private UserResolverBackdoor userRequestAuthorizer;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentDbBackdoor paymentDbBackdoor;

    @Autowired
    private Payment2Repository paymentRepository;

    @Autowired
    private PaymentAuditDeletionRepository paymentAuditDeletionRepository;

    @MockitoBean
    private BarPaymentDeletionService barPaymentDeletionService;

    private RestActions restActions;

    @BeforeEach
    void setUp() {
        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);
        restActions.withAuthorizedService("disposer-fee-and-pay");
    }

    @AfterEach
    void tearDown() {
        this.restActions = null;
    }

    @Test
    void deleteCasePaymentsReturns204AndDeletesPaymentDataAndWritesSuccessAudit() throws Exception {
        PaymentFeeLink paymentFeeLink = paymentDbBackdoor.create(
            PaymentFeeLink.paymentFeeLinkWith()
                .paymentReference("2018-15202505035")
                .ccdCaseNumber(CCD_CASE_NUMBER)
                .payments(Collections.singletonList(
                    Payment.paymentWith()
                        .amount(new BigDecimal("99.99"))
                        .ccdCaseNumber(CCD_CASE_NUMBER)
                        .reference("RC-1632-3254-9172-5888")
                        .paymentStatus(PaymentStatus.paymentStatusWith().name("success").build())
                        .paymentChannel(PaymentChannel.paymentChannelWith().name("online").build())
                        .paymentMethod(PaymentMethod.paymentMethodWith().name("card").build())
                        .build()
                ))
        );
        assertThat(paymentFeeLink.getId()).isNotNull();
        assertThat(paymentRepository.findByCcdCaseNumber(CCD_CASE_NUMBER)
            .orElse(Collections.emptyList())).isNotEmpty();

        restActions
            .delete("/cases/" + CCD_CASE_NUMBER + "/payments")
            .andExpect(status().isNoContent());

        assertThat(paymentRepository.findByCcdCaseNumber(CCD_CASE_NUMBER)
            .orElse(Collections.emptyList())).isEmpty();

        Optional<PaymentAuditDeletion> auditDeletion =
            paymentAuditDeletionRepository.findFirstByCcdCaseNumberOrderByDeletionDateDesc(CCD_CASE_NUMBER);
        assertThat(auditDeletion).isPresent();
        assertThat(auditDeletion.get().getCcdCaseNumber()).isEqualTo(CCD_CASE_NUMBER);
        assertThat(auditDeletion.get().getSourceSystem()).isEqualTo("Payment Disposer");
        assertThat(auditDeletion.get().getSuccess()).isTrue();
        assertThat(auditDeletion.get().getErrorMessage()).isNull();
        verify(barPaymentDeletionService).deleteBarPaymentsByCcdCaseNumber(CCD_CASE_NUMBER);
    }

    @Test
    void deleteCasePaymentsReturns204WhenNoPaymentDataExistsAndWritesSuccessAudit() throws Exception {
        restActions
            .delete("/cases/9999999999999999/payments")
            .andExpect(status().isNoContent());

        Optional<PaymentAuditDeletion> auditDeletion =
            paymentAuditDeletionRepository.findFirstByCcdCaseNumberOrderByDeletionDateDesc("9999999999999999");
        assertThat(auditDeletion).isPresent();
        assertThat(auditDeletion.get().getSuccess()).isTrue();
        assertThat(auditDeletion.get().getSourceSystem()).isEqualTo("Payment Disposer");
        verify(barPaymentDeletionService).deleteBarPaymentsByCcdCaseNumber("9999999999999999");
    }

    @Test
    void recordPaymentDeletionAuditsReturns201AndWritesFailureAndSuccessEntries() throws Exception {
        restActions
            .post("/cases/payments/deletion-audit",
                List.of(
                    Map.of(
                        "ccd_case_number", CCD_CASE_NUMBER,
                        "source_system", "Payment Disposer",
                        "error_message", "Exception message",
                        "success", false
                    ),
                    Map.of(
                        "ccd_case_number", "9999999999999999",
                        "source_system", "Payment Disposer",
                        "success", true
                    )
                ))
            .andExpect(status().isCreated());

        Optional<PaymentAuditDeletion> failureAudit =
            paymentAuditDeletionRepository.findFirstByCcdCaseNumberOrderByDeletionDateDesc(CCD_CASE_NUMBER);
        assertThat(failureAudit).isPresent();
        assertThat(failureAudit.get().getCcdCaseNumber()).isEqualTo(CCD_CASE_NUMBER);
        assertThat(failureAudit.get().getSourceSystem()).isEqualTo("Payment Disposer");
        assertThat(failureAudit.get().getErrorMessage()).isEqualTo("Exception message");
        assertThat(failureAudit.get().getSuccess()).isFalse();
        assertThat(failureAudit.get().getDeletionDate()).isNotNull();

        Optional<PaymentAuditDeletion> successAudit =
            paymentAuditDeletionRepository.findFirstByCcdCaseNumberOrderByDeletionDateDesc("9999999999999999");
        assertThat(successAudit).isPresent();
        assertThat(successAudit.get().getSuccess()).isTrue();
        assertThat(successAudit.get().getErrorMessage()).isNull();
    }
}
