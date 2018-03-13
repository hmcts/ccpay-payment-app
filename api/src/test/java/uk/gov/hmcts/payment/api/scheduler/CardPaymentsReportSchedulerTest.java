package uk.gov.hmcts.payment.api.scheduler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.fees2.register.api.contract.Fee2Dto;
import uk.gov.hmcts.payment.api.fees.client.FeesRegisterClient;

import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
@RunWith(MockitoJUnitRunner.class)
public class CardPaymentsReportSchedulerTest {


    @Mock
    private PaymentsReportService paymentsReportService;

    @Mock
    private FeesRegisterClient feesRegisterClient;
    private Map<String,Fee2Dto>  feesDataMap = Collections.emptyMap();

    private CardPaymentsReportScheduler cardPaymentsReportScheduler;


    @Before
    public void setUp() {
        cardPaymentsReportScheduler = new CardPaymentsReportScheduler(paymentsReportService,feesRegisterClient);
    }

    @Test
    public void shouldInvokeGenerateCardPaymentsReport() {
        // given

        // when
        cardPaymentsReportScheduler.generateCardPaymentsReportTask();

        // then
        verify(feesRegisterClient,times(1)).getFeesDataAsMap();
        verify(paymentsReportService).generateCardPaymentsCsvAndSendEmail(null, null,feesDataMap);
    }

}

