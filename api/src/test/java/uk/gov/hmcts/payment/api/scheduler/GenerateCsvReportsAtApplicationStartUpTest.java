package uk.gov.hmcts.payment.api.scheduler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import uk.gov.hmcts.payment.api.reports.PaymentsReportService;

import static org.mockito.Mockito.verify;
@RunWith(MockitoJUnitRunner.class)
public class GenerateCsvReportsAtApplicationStartUpTest {

    private GenerateCsvReportsAtApplicationStartUp generateCsvReportsAtApplicationStartUp;

    @Mock
    private PaymentsReportService paymentsReportService;

    @Mock
    ApplicationReadyEvent applicationReadyEvent;

    @Before
    public void setUp() {

        generateCsvReportsAtApplicationStartUp= new GenerateCsvReportsAtApplicationStartUp(paymentsReportService);

    }

    @Test
    public void shouldGeneratePaymentsCsvAndSendEmail()  {
        // given

        // when
        generateCsvReportsAtApplicationStartUp.onApplicationEvent(applicationReadyEvent);

        // then
        verify(paymentsReportService).generateCardPaymentsCsvAndSendEmail(null,null);
        verify(paymentsReportService).generateCreditAccountPaymentsCsvAndSendEmail(null,null);


    }


}

