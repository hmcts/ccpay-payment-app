package uk.gov.hmcts.payment.api.reports.config;

import org.junit.Test;
import uk.gov.hmcts.payment.api.reports.PaymentReportType;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CardPaymentReportConfigTest {
    CardPaymentReportConfig cardPaymentReportConfig = new CardPaymentReportConfig();

    @Test
    public void testGetType(){
        PaymentReportType type = cardPaymentReportConfig.getType();
        assertEquals(PaymentReportType.CARD, type);
    }

    @Test
    public void testGetCsvHeader(){
        String expected =  "Service,Payment Group reference,Payment reference," +
            "CCD reference,Case reference,Payment created date,Payment status updated date,Payment status," +
            "Payment channel,Payment method,Payment amount,Site id,Fee code,Version,Calculated amount,Memo line,NAC," +
            "Fee volume";
        String response = cardPaymentReportConfig.getCsvHeader();
        assertEquals(expected,response);
    }

    @Test
    public void testGetCsvFileNamePrefix(){
        String expected = "hmcts_card_payments_";
        String response = cardPaymentReportConfig.getCsvFileNamePrefix();
        assertEquals(expected,response);
    }
}
