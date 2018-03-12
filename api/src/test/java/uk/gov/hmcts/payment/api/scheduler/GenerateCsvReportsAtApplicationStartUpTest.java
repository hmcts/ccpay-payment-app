package uk.gov.hmcts.payment.api.scheduler;

import org.joda.time.MutableDateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.mockito.Mockito.verify;
@RunWith(MockitoJUnitRunner.class)
public class GenerateCsvReportsAtApplicationStartUpTest {

    private GenerateCsvReportsAtApplicationStartUp generateCsvReportsAtApplicationStartUp;

    @Mock
    private PaymentsReportService paymentsReportService;

    @Mock
    ApplicationReadyEvent applicationReadyEvent;

    @Before
    public void setUp() throws ParseException {

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

