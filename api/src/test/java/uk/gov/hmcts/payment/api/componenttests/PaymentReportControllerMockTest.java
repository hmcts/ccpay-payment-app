package uk.gov.hmcts.payment.api.componenttests;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.payment.api.controllers.PaymentReportController;
import uk.gov.hmcts.payment.api.scheduler.CardPaymentsReportScheduler;
import uk.gov.hmcts.payment.api.scheduler.CreditAccountPaymentsReportScheduler;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class PaymentReportControllerMockTest {

    private MockMvc mockMvc;

    @Mock
    private CardPaymentsReportScheduler cardPaymentsReportScheduler;
    @Mock
    private CreditAccountPaymentsReportScheduler creditAccountPaymentsReportScheduler;

    @InjectMocks
    private PaymentReportController controller;

    @Before
    public void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void cardPaymentReport() throws Exception {
        // given
        ReflectionTestUtils.setField(controller, "cardReportsEnabled", true);
        // when & then
        this.mockMvc.perform(post("/payment-csv-reports"))
            .andExpect(status().isOk());

        verify(cardPaymentsReportScheduler).generateCardPaymentsReportTask();
    }

    @Test
    public void pbaPaymentReport() throws Exception {
        // given
        ReflectionTestUtils.setField(controller, "pbaReportsEnabled", true);
        // when & then
        this.mockMvc.perform(post("/payment-csv-reports"))
            .andExpect(status().isOk());

        verify(creditAccountPaymentsReportScheduler).generateCreditAccountPaymentsReportTask();
    }
}
