package uk.gov.hmcts.payment.api.scheduler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;
@RunWith(MockitoJUnitRunner.class)
public class CreditAccountPaymentsReportSchedulerTest {

    @Mock
    private PaymentsReportService paymentsReportService;

    private CreditAccountPaymentsReportScheduler creditAccountPaymentsReportScheduler;

    @Before
    public void setUp() {
        creditAccountPaymentsReportScheduler = new CreditAccountPaymentsReportScheduler(paymentsReportService);
    }

    @Test
    public void shouldInvokeGenerateCreditAccountPaymentsReport()  {
        // given

        // when
        creditAccountPaymentsReportScheduler.generateCreditAccountPaymentsReportTask();

        // then
        verify(paymentsReportService).generateCreditAccountPaymentsCsvAndSendEmail(null,null);
    }
}

