package uk.gov.hmcts.payment.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AntennaTelephonySystemTest {

    private TelephonySystem telephonySystem;

    @BeforeEach
    void setUp() {
        telephonySystem = AntennaTelephonySystem.builder()
            .probateFlowId("mockProbateAntennaFlowId")
            .divorceFlowId("mockDivorceAntennaFlowId")
            .strategicFlowId("mockStrategicAntennaFlowId")
            .iacFlowId("mockIacAntennaFlowId")
            .prlFlowId("mockPrlAntennaFlowId")
            .build();
    }

    @Test
    void shouldReturnCorrectFlowIdForProbate() {
        String flowId = telephonySystem.getFlowId("Probate");
        assertEquals("mockProbateAntennaFlowId", flowId);
    }

    @Test
    void shouldReturnCorrectFlowIdForDivorce() {
        String flowId = telephonySystem.getFlowId("Divorce");
        assertEquals("mockDivorceAntennaFlowId", flowId);
    }

    @Test
    void shouldThrowExceptionForUnsupportedServiceType() {
        // Act & Assert
        PaymentException exception = assertThrows(PaymentException.class,
            () -> telephonySystem.getFlowId("UnsupportedService"));
        assertEquals("This telephony system does not support telephony calls for the service 'UnsupportedService'.", exception.getMessage());
    }
}
