package uk.gov.hmcts.payment.api.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentDtoMapper;
import uk.gov.hmcts.payment.api.dto.mapper.PaymentGroupDtoMapper;
import uk.gov.hmcts.payment.api.service.BarPaymentDeletionService;
import uk.gov.hmcts.payment.api.service.CasePaymentDeletionService;
import uk.gov.hmcts.payment.api.service.PaymentDeletionAuditDetails;
import uk.gov.hmcts.payment.api.service.PaymentDeletionAuditService;
import uk.gov.hmcts.payment.api.service.PaymentGroupService;
import uk.gov.hmcts.payment.api.service.PaymentService;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CasePaymentDeletionControllerTest {

    private static final String CCD_CASE_NUMBER = "1747738470383168";

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentGroupService paymentGroupService;

    @Mock
    private PaymentDtoMapper paymentDtoMapper;

    @Mock
    private PaymentGroupDtoMapper paymentGroupDtoMapper;

    @Mock
    private CasePaymentDeletionService casePaymentDeletionService;

    @Mock
    private BarPaymentDeletionService barPaymentDeletionService;

    @Mock
    private PaymentDeletionAuditService paymentDeletionAuditService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CaseController caseController = new CaseController(
            paymentService,
            paymentGroupService,
            paymentDtoMapper,
            paymentGroupDtoMapper,
            casePaymentDeletionService,
            barPaymentDeletionService,
            paymentDeletionAuditService
        );

        mockMvc = MockMvcBuilders.standaloneSetup(caseController).build();
    }

    @Test
    void deleteCasePaymentsReturns204AndDeletesBarThenPaymentDataThenWritesSuccessAudit() throws Exception {
        mockMvc.perform(delete("/cases/{ccd-case-number}/payments", CCD_CASE_NUMBER)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        var order = inOrder(barPaymentDeletionService, casePaymentDeletionService, paymentDeletionAuditService);
        order.verify(barPaymentDeletionService).deleteBarPaymentsByCcdCaseNumber(CCD_CASE_NUMBER);
        order.verify(casePaymentDeletionService).deletePaymentsByCcdCaseNumber(CCD_CASE_NUMBER);
        order.verify(paymentDeletionAuditService).recordDeletionAudits(eq(List.of(
            new PaymentDeletionAuditDetails(CCD_CASE_NUMBER, "Payment Disposer", null, true)
        )));
    }

    @Test
    void recordPaymentDeletionAuditsReturns201AndDelegatesToAuditService() throws Exception {
        mockMvc.perform(post("/cases/payments/deletion-audit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[{"
                    + "\"ccd_case_number\":\"" + CCD_CASE_NUMBER + "\","
                    + "\"source_system\":\"Payment Disposer\","
                    + "\"error_message\":\"Exception message\","
                    + "\"success\":false"
                    + "}]"))
            .andExpect(status().isCreated());

        verify(paymentDeletionAuditService).recordDeletionAudits(eq(List.of(
            new PaymentDeletionAuditDetails(
                CCD_CASE_NUMBER, "Payment Disposer", "Exception message", false)
        )));
    }
}
