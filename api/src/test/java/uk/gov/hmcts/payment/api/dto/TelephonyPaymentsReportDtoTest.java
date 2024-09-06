// TelephonyPaymentsReportDtoTest.java
package uk.gov.hmcts.payment.api.dto;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class TelephonyPaymentsReportDtoTest {

    @Test
    public void testTelephonyPaymentsReportDto() {
        Date paymentDate = new Date();
        TelephonyPaymentsReportDto dto = TelephonyPaymentsReportDto.telephonyPaymentsReportDtoWith()
            .serviceName("Service1")
            .ccdReference("CCD123")
            .paymentReference("PAY123")
            .feeCode("FEE001")
            .paymentDate(paymentDate)
            .Amount(new BigDecimal("100.00"))
            .paymentStatus("Success")
            .build();

        assertEquals("Service1", dto.getServiceName());
        assertEquals("CCD123", dto.getCcdReference());
        assertEquals("PAY123", dto.getPaymentReference());
        assertEquals("FEE001", dto.getFeeCode());
        assertEquals(paymentDate, dto.getPaymentDate());
        assertEquals(new BigDecimal("100.00"), dto.getAmount());
        assertEquals("Success", dto.getPaymentStatus());
    }
}
