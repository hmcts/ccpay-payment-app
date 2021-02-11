package uk.gov.hmcts.payment.api.reports.config;

import org.junit.Test;
import uk.gov.hmcts.payment.api.reports.PaymentReportType;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BarPaymentReportConfigTest {

    BarPaymentReportConfig barPaymentReportConfig = new BarPaymentReportConfig();

    @Test
    public void testGetType(){
        PaymentReportType type = barPaymentReportConfig.getType();
        assertEquals(PaymentReportType.DIGITAL_BAR, type);
    }

    @Test
    public void testGetCsvHeader(){
        String expected = "Service,Payment Group reference,Payment reference," +
            "CCD reference,Case reference,Organisation name,Customer internal reference,PBA Number,Payment created date," +
            "Payment status updated date,Payment status,Payment channel,Payment method,Payment amount,Site id,Fee code," +
            "Version,Calculated amount,Memo line,NAC,Fee volume";
        String response = barPaymentReportConfig.getCsvHeader();
        assertEquals(expected,response);
    }

    @Test
    public void testGetCsvFileNamePrefix(){
        String expected = "hmcts_bar_payments_";
        String response = barPaymentReportConfig.getCsvFileNamePrefix();
        assertEquals(expected,response);
    }
}
