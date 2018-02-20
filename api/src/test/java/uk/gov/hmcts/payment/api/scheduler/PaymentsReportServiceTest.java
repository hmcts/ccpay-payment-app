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


    private CardPaymentsReportScheduler cardPaymentsReportScheduler;

    private CreditAccountPaymentsReportScheduler creditAccountPaymentsReportScheduler;

    @Before
    public void setUp() {
        cardPaymentsReportScheduler = new CardPaymentsReportScheduler(paymentsReportService);
        creditAccountPaymentsReportScheduler =new CreditAccountPaymentsReportScheduler(paymentsReportService);
    }

    @Test
    public void shouldInvokeGenerateCardPaymentsReport() throws Exception {
        // given

        // when
        cardPaymentsReportScheduler.generateCardPaymentsReport();

        // then
        verify(paymentsReportService).generateCardPaymentsCsv(null,null);
    }

    @Test
    public void shouldInvokeGenerateCreditAccountPaymentsReport() throws Exception {
        // given

        // when
        creditAccountPaymentsReportScheduler.generateCreditAccountPaymentsReport();

        // then
        verify(paymentsReportService).generateCreditAccountPaymentsCsv(null,null);
    }



}
