package uk.gov.hmcts.payment.api.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BarPaymentDeletionServiceImplTest {

    private static final String CASE_REFERENCE = "1747738470383168";

    @Mock
    private JdbcTemplate barJdbcTemplate;

    @InjectMocks
    private BarPaymentDeletionServiceImpl barPaymentDeletionService;

    @Test
    void deleteBarPaymentsByCcdCaseNumberDeletesInCorrectOrder() {
        List<Integer> paymentInstructionIds = Arrays.asList(123456, 123457);
        when(barJdbcTemplate.queryForList(anyString(), eq(Integer.class), eq(CASE_REFERENCE)))
            .thenReturn(paymentInstructionIds);

        barPaymentDeletionService.deleteBarPaymentsByCcdCaseNumber(CASE_REFERENCE);

        verify(barJdbcTemplate).queryForList(
            "SELECT DISTINCT payment_instruction_id FROM case_fee_detail WHERE case_reference = ?",
            Integer.class,
            CASE_REFERENCE
        );
        verify(barJdbcTemplate).update(
            "DELETE FROM payment_instruction_status WHERE payment_instruction_id IN "
                + "(SELECT payment_instruction_id FROM case_fee_detail WHERE case_reference = ?)",
            CASE_REFERENCE
        );
        verify(barJdbcTemplate).update(
            "DELETE FROM payment_instruction_payhub_reference WHERE payment_instruction_id IN "
                + "(SELECT payment_instruction_id FROM case_fee_detail WHERE case_reference = ?)",
            CASE_REFERENCE
        );
        verify(barJdbcTemplate).update(
            "DELETE FROM case_fee_detail WHERE case_reference = ?",
            CASE_REFERENCE
        );
        verify(barJdbcTemplate).update("DELETE FROM payment_instruction WHERE id = ?", 123456);
        verify(barJdbcTemplate).update("DELETE FROM payment_instruction WHERE id = ?", 123457);
    }

    @Test
    void deleteBarPaymentsByCcdCaseNumberDoesNothingWhenNoInstructionsFound() {
        when(barJdbcTemplate.queryForList(anyString(), eq(Integer.class), eq(CASE_REFERENCE)))
            .thenReturn(Collections.emptyList());

        barPaymentDeletionService.deleteBarPaymentsByCcdCaseNumber(CASE_REFERENCE);

        verify(barJdbcTemplate, never()).update(anyString(), eq(CASE_REFERENCE));
        verify(barJdbcTemplate, never()).update(anyString(), eq(123456));
    }
}
