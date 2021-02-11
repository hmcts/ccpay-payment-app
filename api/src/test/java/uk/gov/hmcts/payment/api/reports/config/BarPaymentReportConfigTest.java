package uk.gov.hmcts.payment.api.reports.config;

import org.junit.Test;
import uk.gov.hmcts.payment.api.reports.PaymentReportType;

public class BarPaymentReportConfigTest {


    BarPaymentReportConfig barPaymentReportConfig = new BarPaymentReportConfig();

    @Test
    public void testGetType(){
        PaymentReportType type = barPaymentReportConfig.getType();
    }
}
