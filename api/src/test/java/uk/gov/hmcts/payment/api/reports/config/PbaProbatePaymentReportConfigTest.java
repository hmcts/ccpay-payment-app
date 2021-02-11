package uk.gov.hmcts.payment.api.reports.config;

import org.junit.Test;
import uk.gov.hmcts.payment.api.reports.PaymentReportType;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PbaProbatePaymentReportConfigTest {
    PbaProbatePaymentReportConfig pbaProbatePaymentReportConfig = new PbaProbatePaymentReportConfig();

    @Test
    public void testGetType(){
        PaymentReportType type = pbaProbatePaymentReportConfig.getType();
        assertEquals(PaymentReportType.PBA_PROBATE, type);
    }

    @Test
    public void testGetCsvHeader(){
        String expected = "Service,Payment Group reference,Payment reference," +
            "CCD reference,Case reference,Organisation name,Customer internal reference,PBA Number,Payment created date," +
            "Payment status updated date,Payment status,Payment channel,Payment method,Payment amount,Site id,Fee code," +
            "Version,Calculated amount,Memo line,NAC,Fee volume";
        String response = pbaProbatePaymentReportConfig.getCsvHeader();
        assertEquals(expected,response);
    }

    @Test
    public void testGetCsvFileNamePrefix(){
        String expected = "hmcts_credit_account_payments_probate_";
        String response = pbaProbatePaymentReportConfig.getCsvFileNamePrefix();
        assertEquals(expected,response);
    }
}
