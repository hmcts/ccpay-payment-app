package uk.gov.hmcts.payment.api.scheduler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.payment.api.reports.FeesService;
import uk.gov.hmcts.payment.api.reports.PaymentsReportService;

import static org.mockito.Mockito.verify;
@RunWith(MockitoJUnitRunner.class)
public class CreditAccountPaymentsReportSchedulerTest {

    @Mock
    private PaymentsReportService paymentsReportService;

    @Mock
    private FeesService feesService;

    private CreditAccountPaymentsReportScheduler creditAccountPaymentsReportScheduler;

    @Before
    public void setUp() {
        creditAccountPaymentsReportScheduler = new CreditAccountPaymentsReportScheduler(paymentsReportService,feesService);
    }

    @Test
    public void shouldInvokeGenerateCreditAccountPaymentsReport()  {
        // given

        // when
        creditAccountPaymentsReportScheduler.generateCreditAccountPaymentsReportTask();

        // then
        verify(feesService).dailyRefreshOfFeesData();
        verify(paymentsReportService).generateCreditAccountPaymentsCsvAndSendEmail(null,null);
    }
}

