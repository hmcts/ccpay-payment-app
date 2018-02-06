package uk.gov.hmcts.payment.api.scheduler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PaymentsReportServiceTest {

    @Mock
    private PaymentsReportService paymentsReportService;


    private PaymentsReportScheduler scheduler;

    @Before
    public void setUp() {
        scheduler = new PaymentsReportScheduler(paymentsReportService);
    }

    @Test
    public void shouldInvokeCsvExtractServiceToExtractCsv() throws Exception {
        // given

        // when
        scheduler.generatePaymentsReport();

        // then
        verify(paymentsReportService).generateCsv(null,null);
    }
}








