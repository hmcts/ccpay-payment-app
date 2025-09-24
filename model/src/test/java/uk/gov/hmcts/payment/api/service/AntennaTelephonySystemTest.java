package uk.gov.hmcts.payment.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.payment.api.util.TestUtil.setPrivateField;

class AntennaTelephonySystemTest {

    private AntennaTelephonySystem antennaTelephonySystem;

    @BeforeEach
    void setUp() {
        antennaTelephonySystem = new AntennaTelephonySystem();
        setPrivateField(antennaTelephonySystem, "antennaProbateFlowId", "mockProbateAntennaFlowId");
        setPrivateField(antennaTelephonySystem, "antennaDivorceFlowId", "mockDivorceAntennaFlowId");
        setPrivateField(antennaTelephonySystem, "antennaStrategicFlowId", "mockStrategicAntennaFlowId");
        setPrivateField(antennaTelephonySystem, "antennaIacFlowId", "mockIacAntennaFlowId");
        setPrivateField(antennaTelephonySystem, "antennaPrlFlowId", "mockPrlAntennaFlowId");
    }

    @Test
    void shouldReturnCorrectFlowIdForProbate() {
        TelephonySystem telephonySystem = antennaTelephonySystem;
        String flowId = telephonySystem.getFlowId("Probate");
        assertEquals("mockProbateAntennaFlowId", flowId);
    }

    @Test
    void shouldReturnCorrectFlowIdForDivorce() {
        TelephonySystem telephonySystem = antennaTelephonySystem;
        String flowId = telephonySystem.getFlowId("Divorce");
        assertEquals("mockDivorceAntennaFlowId", flowId);
    }

    @Test
    void shouldThrowExceptionForUnsupportedServiceType() {
        // Act & Assert
        TelephonySystem telephonySystem = antennaTelephonySystem;
        PaymentException exception = assertThrows(PaymentException.class,
            () -> telephonySystem.getFlowId("UnsupportedService"));
        assertEquals("This telephony system does not support telephony calls for the service 'UnsupportedService'.", exception.getMessage());
    }
}
