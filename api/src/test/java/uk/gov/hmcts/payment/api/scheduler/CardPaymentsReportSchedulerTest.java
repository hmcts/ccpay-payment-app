package uk.gov.hmcts.payment.api.scheduler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;
@RunWith(MockitoJUnitRunner.class)
public class CardPaymentsReportSchedulerTest {


    @Mock
    private PaymentsReportService paymentsReportService;

    private CardPaymentsReportScheduler cardPaymentsReportScheduler;


    @Before
    public void setUp() {
        cardPaymentsReportScheduler = new CardPaymentsReportScheduler(paymentsReportService);
    }

    @Test
    public void shouldInvokeGenerateCardPaymentsReport() {
        // given

        // when
        cardPaymentsReportScheduler.generateCardPaymentsReportTask();

        // then
        verify(paymentsReportService).generateCardPaymentsCsvAndSendEmail(null, null);
    }

}

