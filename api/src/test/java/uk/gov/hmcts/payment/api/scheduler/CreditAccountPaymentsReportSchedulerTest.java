package uk.gov.hmcts.payment.api.scheduler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.payment.api.reports.FeesService;
import uk.gov.hmcts.payment.api.reports.PaymentsReportService;

import java.util.Date;

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

        Date fromDate = new Date();
        Date toDate = new Date();

        // when
        creditAccountPaymentsReportScheduler.generateCreditAccountPaymentsReportTask(fromDate, toDate);

        // then
        verify(feesService).dailyRefreshOfFeesData();
        verify(paymentsReportService).generateCreditAccountPaymentsCsvAndSendEmail(fromDate, toDate);
    }
}

