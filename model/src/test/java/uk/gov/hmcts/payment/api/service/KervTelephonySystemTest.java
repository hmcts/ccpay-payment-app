package uk.gov.hmcts.payment.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KervTelephonySystemTest {

    private TelephonySystem telephonySystem;

    @BeforeEach
    void setUp() {
        telephonySystem = KervTelephonySystem.builder()
            .probateFlowId("mockProbateKervFlowId")
            .divorceFlowId("mockDivorceKervFlowId")
            .strategicFlowId("mockStrategicKervFlowId")
            .iacFlowId("mockIacKervFlowId")
            .prlFlowId("mockPrlKervFlowId")
            .build();
    }

    @Test
    void shouldReturnCorrectFlowIdForProbate() {
        String flowId = telephonySystem.getFlowId("Probate");
        assertEquals("mockProbateKervFlowId", flowId);
    }

    @Test
    void shouldReturnCorrectFlowIdForDivorce() {
        String flowId = telephonySystem.getFlowId("Divorce");
        assertEquals("mockDivorceKervFlowId", flowId);
    }

    @Test
    void shouldThrowExceptionForUnsupportedServiceType() {
        // Act & Assert
        PaymentException exception = assertThrows(PaymentException.class,
            () -> telephonySystem.getFlowId("UnsupportedService"));
        assertEquals("This telephony system does not support telephony calls for the service 'UnsupportedService'.", exception.getMessage());
    }
}
