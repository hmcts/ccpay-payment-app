package uk.gov.hmcts.payment.api.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CasePaymentDeletionServiceImplTest {

    private static final String CCD_CASE_NUMBER = "1747738470383168";

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private CasePaymentDeletionServiceImpl casePaymentDeletionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(casePaymentDeletionService, "entityManager", entityManager);
    }

    @Test
    void deletePaymentsByCcdCaseNumberDeletesRelatedRecords() {
        Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(1);

        casePaymentDeletionService.deletePaymentsByCcdCaseNumber(CCD_CASE_NUMBER);

        verify(entityManager, times(10)).createNativeQuery(anyString());
        verify(query, times(10)).setParameter("ccdCaseNumber", CCD_CASE_NUMBER);
        verify(query, times(10)).executeUpdate();
    }

    @Test
    void deletePaymentsByCcdCaseNumberIsIdempotentWhenNoRowsDeleted() {
        Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(0);

        casePaymentDeletionService.deletePaymentsByCcdCaseNumber(CCD_CASE_NUMBER);

        verify(query, times(10)).executeUpdate();
    }
}
