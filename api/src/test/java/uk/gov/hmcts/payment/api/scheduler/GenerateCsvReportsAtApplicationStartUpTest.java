package uk.gov.hmcts.payment.api.scheduler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import uk.gov.hmcts.fees2.register.api.contract.Fee2Dto;
import uk.gov.hmcts.payment.api.fees.client.FeesRegisterClient;

import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
@RunWith(MockitoJUnitRunner.class)
public class GenerateCsvReportsAtApplicationStartUpTest {

    private GenerateCsvReportsAtApplicationStartUp generateCsvReportsAtApplicationStartUp;

    @Mock
    private PaymentsReportService paymentsReportService;

    @Mock
    ApplicationReadyEvent applicationReadyEvent;

    @Mock
    FeesRegisterClient feesRegisterClient;

    private Map<String,Fee2Dto> feesDataMap = Collections.emptyMap();


    @Before
    public void setUp() {

        generateCsvReportsAtApplicationStartUp= new GenerateCsvReportsAtApplicationStartUp(paymentsReportService,feesRegisterClient);

    }

    @Test
    public void shouldGeneratePaymentsCsvAndSendEmail()  {
        // given

        // when
        generateCsvReportsAtApplicationStartUp.onApplicationEvent(applicationReadyEvent);

        // then
        verify(feesRegisterClient,times(1)).getFeesDataAsMap();
        verify(paymentsReportService).generateCardPaymentsCsvAndSendEmail(null,null,feesDataMap);
        verify(paymentsReportService).generateCreditAccountPaymentsCsvAndSendEmail(null,null,feesDataMap);


    }


}

