package uk.gov.hmcts.payment.api.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.payment.api.model.PaymentAuditDeletion;
import uk.gov.hmcts.payment.api.model.PaymentAuditDeletionRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentDeletionAuditServiceImplTest {

    private static final String CCD_CASE_NUMBER = "1747738470383168";
    private static final String SOURCE_SYSTEM = "Payment Disposer";

    @Mock
    private PaymentAuditDeletionRepository paymentAuditDeletionRepository;

    @InjectMocks
    private PaymentDeletionAuditServiceImpl paymentDeletionAuditService;

    @Test
    void recordDeletionAuditsPersistsFailureAndSuccessEntries() {
        when(paymentAuditDeletionRepository.saveAll(anyList()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        paymentDeletionAuditService.recordDeletionAudits(List.of(
            new PaymentDeletionAuditDetails(CCD_CASE_NUMBER, SOURCE_SYSTEM, "Exception message", false),
            new PaymentDeletionAuditDetails("9999999999999999", SOURCE_SYSTEM, null, true)
        ));

        ArgumentCaptor<List<PaymentAuditDeletion>> auditCaptor = ArgumentCaptor.forClass(List.class);
        verify(paymentAuditDeletionRepository).saveAll(auditCaptor.capture());
        assertThat(auditCaptor.getValue()).hasSize(2);
        assertThat(auditCaptor.getValue().get(0).getCcdCaseNumber()).isEqualTo(CCD_CASE_NUMBER);
        assertThat(auditCaptor.getValue().get(0).getSourceSystem()).isEqualTo(SOURCE_SYSTEM);
        assertThat(auditCaptor.getValue().get(0).getErrorMessage()).isEqualTo("Exception message");
        assertThat(auditCaptor.getValue().get(0).getSuccess()).isFalse();
        assertThat(auditCaptor.getValue().get(1).getSuccess()).isTrue();
        assertThat(auditCaptor.getValue().get(1).getErrorMessage()).isNull();
    }

    @Test
    void recordDeletionAuditsTruncatesErrorMessageTo500Characters() {
        when(paymentAuditDeletionRepository.saveAll(anyList()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        String longMessage = "x".repeat(600);

        paymentDeletionAuditService.recordDeletionAudits(List.of(
            new PaymentDeletionAuditDetails(CCD_CASE_NUMBER, SOURCE_SYSTEM, longMessage, false)
        ));

        ArgumentCaptor<List<PaymentAuditDeletion>> auditCaptor = ArgumentCaptor.forClass(List.class);
        verify(paymentAuditDeletionRepository).saveAll(auditCaptor.capture());
        assertThat(auditCaptor.getValue().get(0).getErrorMessage()).hasSize(500);
    }
}
