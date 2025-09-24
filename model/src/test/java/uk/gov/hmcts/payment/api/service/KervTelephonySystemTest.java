package uk.gov.hmcts.payment.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.payment.api.util.TestUtil.setPrivateField;

class KervTelephonySystemTest {

    private KervTelephonySystem kervTelephonySystem;

    @BeforeEach
    void setUp() {
        kervTelephonySystem = new KervTelephonySystem();
        setPrivateField(kervTelephonySystem, "kervProbateFlowId", "mockProbateKervFlowId");
        setPrivateField(kervTelephonySystem, "kervDivorceFlowId", "mockDivorceKervFlowId");
        setPrivateField(kervTelephonySystem, "kervStrategicFlowId", "mockStrategicKervFlowId");
        setPrivateField(kervTelephonySystem, "kervIacFlowId", "mockIacKervFlowId");
        setPrivateField(kervTelephonySystem, "kervPrlFlowId", "mockPrlKervFlowId");
    }

    @Test
    void shouldReturnCorrectFlowIdForProbate() throws JsonProcessingException {
        TelephonySystem telephonySystem = kervTelephonySystem;
        String flowId = telephonySystem.getFlowId("Probate");
        assertEquals("mockProbateKervFlowId", flowId);
    }

    @Test
    void shouldReturnCorrectFlowIdForDivorce() {
        TelephonySystem telephonySystem = kervTelephonySystem;
        String flowId = telephonySystem.getFlowId("Divorce");
        assertEquals("mockDivorceKervFlowId", flowId);
    }

    @Test
    void shouldThrowExceptionForUnsupportedServiceType() {
        // Act & Assert
        TelephonySystem telephonySystem = kervTelephonySystem;
        PaymentException exception = assertThrows(PaymentException.class,
            () -> telephonySystem.getFlowId("UnsupportedService"));
        assertEquals("This telephony system does not support telephony calls for the service 'UnsupportedService'.", exception.getMessage());
    }



}
